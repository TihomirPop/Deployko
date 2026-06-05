package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.port.in.DeployServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.HandleCiPipelineCompletedEventUseCase;
import hr.tvz.popovic.deployko.application.port.out.FindLastCiDeploymentPort;
import hr.tvz.popovic.deployko.application.port.out.RecordCiDeploymentPort;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

public final class CiPipelineCompletedEventDomainService implements HandleCiPipelineCompletedEventUseCase {

    private final FindLastCiDeploymentPort findLastCiDeploymentPort;
    private final RecordCiDeploymentPort recordCiDeploymentPort;
    private final DeployServiceUseCase deployServiceUseCase;
    private final Clock clock;
    private final Duration deploymentThrottleWindow;

    public CiPipelineCompletedEventDomainService(
            FindLastCiDeploymentPort findLastCiDeploymentPort,
            RecordCiDeploymentPort recordCiDeploymentPort,
            DeployServiceUseCase deployServiceUseCase,
            Clock clock,
            Duration deploymentThrottleWindow
    ) {
        this.findLastCiDeploymentPort = Objects.requireNonNull(
                findLastCiDeploymentPort,
                "findLastCiDeploymentPort must not be null"
        );
        this.recordCiDeploymentPort = Objects.requireNonNull(
                recordCiDeploymentPort,
                "recordCiDeploymentPort must not be null"
        );
        this.deployServiceUseCase = Objects.requireNonNull(deployServiceUseCase, "deployServiceUseCase must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.deploymentThrottleWindow = Objects.requireNonNull(
                deploymentThrottleWindow,
                "deploymentThrottleWindow must not be null"
        );

        if (deploymentThrottleWindow.isNegative() || deploymentThrottleWindow.isZero()) {
            throw new IllegalArgumentException("deploymentThrottleWindow must be positive");
        }
    }

    @Override
    public HandleCiPipelineCompletedEventResult handleCiPipelineCompletedEvent(
            HandleCiPipelineCompletedEventCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");

        OffsetDateTime now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);

        return switch (findLastCiDeploymentPort.findLastCiDeployment(command.serviceName())) {
            case FindLastCiDeploymentPort.FindLastCiDeploymentResult.Found found when isWithinThrottleWindow(
                    found.deployedAt(),
                    now
            ) -> new HandleCiPipelineCompletedEventResult.SkippedRecentDeployment();
            case FindLastCiDeploymentPort.FindLastCiDeploymentResult.Found _,
                 FindLastCiDeploymentPort.FindLastCiDeploymentResult.NotDeployed _ -> deployAndRecord(command, now);
            case FindLastCiDeploymentPort.FindLastCiDeploymentResult.ServiceNotFound _ ->
                    new HandleCiPipelineCompletedEventResult.ServiceNotFound();
            case FindLastCiDeploymentPort.FindLastCiDeploymentResult.Failure _ ->
                    new HandleCiPipelineCompletedEventResult.LastDeploymentLookupFailure();
        };
    }

    private boolean isWithinThrottleWindow(OffsetDateTime lastDeployedAt, OffsetDateTime now) {
        return !lastDeployedAt.plus(deploymentThrottleWindow).isBefore(now);
    }

    private HandleCiPipelineCompletedEventResult deployAndRecord(
            HandleCiPipelineCompletedEventCommand command,
            OffsetDateTime now
    ) {
        return switch (deployServiceUseCase.deployService(new DeployServiceUseCase.DeployServiceCommand(
                command.serviceName(),
                command.imageVersion()
        ))) {
            case DeployServiceUseCase.DeployServiceResult.Success _ -> recordDeployment(command, now);
            case DeployServiceUseCase.DeployServiceResult.ServiceNotFound _ ->
                    new HandleCiPipelineCompletedEventResult.ServiceNotFound();
            case DeployServiceUseCase.DeployServiceResult.DesiredStateFailure _,
                 DeployServiceUseCase.DeployServiceResult.DockerFailure _ ->
                    new HandleCiPipelineCompletedEventResult.DeploymentFailure();
        };
    }

    private HandleCiPipelineCompletedEventResult recordDeployment(
            HandleCiPipelineCompletedEventCommand command,
            OffsetDateTime now
    ) {
        return switch (recordCiDeploymentPort.recordCiDeployment(command.serviceName(), now)) {
            case RecordCiDeploymentPort.RecordCiDeploymentResult.Recorded _ ->
                    new HandleCiPipelineCompletedEventResult.Deployed();
            case RecordCiDeploymentPort.RecordCiDeploymentResult.ServiceNotFound _ ->
                    new HandleCiPipelineCompletedEventResult.ServiceNotFound();
            case RecordCiDeploymentPort.RecordCiDeploymentResult.Failure _ ->
                    new HandleCiPipelineCompletedEventResult.RecordDeploymentFailure();
        };
    }
}
