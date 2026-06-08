package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.DeployServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.HandleCiPipelineCompletedEventUseCase;
import hr.tvz.popovic.deployko.application.port.out.FindLastCiDeploymentPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceNamesByImageRepositoryPort;
import hr.tvz.popovic.deployko.application.port.out.RecordCiDeploymentPort;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

public final class CiPipelineCompletedEventDomainService implements HandleCiPipelineCompletedEventUseCase {

    private final FindServiceNamesByImageRepositoryPort findServiceNamesByImageRepositoryPort;
    private final FindLastCiDeploymentPort findLastCiDeploymentPort;
    private final RecordCiDeploymentPort recordCiDeploymentPort;
    private final DeployServiceUseCase deployServiceUseCase;
    private final Clock clock;
    private final Duration deploymentThrottleWindow;

    public CiPipelineCompletedEventDomainService(
            FindServiceNamesByImageRepositoryPort findServiceNamesByImageRepositoryPort,
            FindLastCiDeploymentPort findLastCiDeploymentPort,
            RecordCiDeploymentPort recordCiDeploymentPort,
            DeployServiceUseCase deployServiceUseCase,
            Clock clock,
            Duration deploymentThrottleWindow
    ) {
        this.findServiceNamesByImageRepositoryPort = Objects.requireNonNull(
                findServiceNamesByImageRepositoryPort,
                "findServiceNamesByImageRepositoryPort must not be null"
        );
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

        return switch (findServiceNamesByImageRepositoryPort.findServiceNamesByImageRepository(command.imageRepository())) {
            case FindServiceNamesByImageRepositoryPort.FindServiceNamesByImageRepositoryResult.Found found ->
                    deployMatchingServices(found.serviceNames(), command, now);
            case FindServiceNamesByImageRepositoryPort.FindServiceNamesByImageRepositoryResult.Failure _ ->
                    new HandleCiPipelineCompletedEventResult.ServiceLookupFailure();
        };
    }

    private HandleCiPipelineCompletedEventResult deployMatchingServices(
            List<ServiceName> serviceNames,
            HandleCiPipelineCompletedEventCommand command,
            OffsetDateTime now
    ) {
        if (serviceNames.isEmpty()) {
            return new HandleCiPipelineCompletedEventResult.NoMatchingServices();
        }

        boolean deployed = false;
        HandleCiPipelineCompletedEventResult failure = null;
        for (ServiceName serviceName : serviceNames) {
            HandleCiPipelineCompletedEventResult result = deployMatchingService(serviceName, command, now);
            switch (result) {
                case HandleCiPipelineCompletedEventResult.Deployed _ -> deployed = true;
                case HandleCiPipelineCompletedEventResult.SkippedRecentDeployment _ -> {
                }
                case HandleCiPipelineCompletedEventResult.NoMatchingServices _,
                     HandleCiPipelineCompletedEventResult.DeploymentImageNotFound _,
                     HandleCiPipelineCompletedEventResult.ServiceLookupFailure _,
                     HandleCiPipelineCompletedEventResult.DeploymentFailure _,
                     HandleCiPipelineCompletedEventResult.LastDeploymentLookupFailure _,
                     HandleCiPipelineCompletedEventResult.RecordDeploymentFailure _ -> {
                    if (failure == null) {
                        failure = result;
                    }
                }
            }
        }

        if (failure != null) {
            return failure;
        }
        if (deployed) {
            return new HandleCiPipelineCompletedEventResult.Deployed();
        }
        return new HandleCiPipelineCompletedEventResult.SkippedRecentDeployment();
    }

    private HandleCiPipelineCompletedEventResult deployMatchingService(
            ServiceName serviceName,
            HandleCiPipelineCompletedEventCommand command,
            OffsetDateTime now
    ) {
        return switch (findLastCiDeploymentPort.findLastCiDeployment(serviceName)) {
            case FindLastCiDeploymentPort.FindLastCiDeploymentResult.Found found when isWithinThrottleWindow(
                    found.deployedAt(),
                    now
            ) -> new HandleCiPipelineCompletedEventResult.SkippedRecentDeployment();
            case FindLastCiDeploymentPort.FindLastCiDeploymentResult.Found _,
                 FindLastCiDeploymentPort.FindLastCiDeploymentResult.NotDeployed _ -> deployAndRecord(
                    serviceName,
                    command,
                    now
            );
            case FindLastCiDeploymentPort.FindLastCiDeploymentResult.ServiceNotFound _ ->
                    new HandleCiPipelineCompletedEventResult.DeploymentFailure();
            case FindLastCiDeploymentPort.FindLastCiDeploymentResult.Failure _ ->
                    new HandleCiPipelineCompletedEventResult.LastDeploymentLookupFailure();
        };
    }

    private boolean isWithinThrottleWindow(OffsetDateTime lastDeployedAt, OffsetDateTime now) {
        return !lastDeployedAt.plus(deploymentThrottleWindow).isBefore(now);
    }

    private HandleCiPipelineCompletedEventResult deployAndRecord(
            ServiceName serviceName,
            HandleCiPipelineCompletedEventCommand command,
            OffsetDateTime now
    ) {
        return switch (deployServiceUseCase.deployService(new DeployServiceUseCase.DeployServiceCommand(
                serviceName,
                command.imageVersion()
        ))) {
            case DeployServiceUseCase.DeployServiceResult.Success _ -> recordDeployment(serviceName, now);
            case DeployServiceUseCase.DeployServiceResult.ServiceNotFound _ ->
                    new HandleCiPipelineCompletedEventResult.DeploymentFailure();
            case DeployServiceUseCase.DeployServiceResult.ImageNotFound _ ->
                    new HandleCiPipelineCompletedEventResult.DeploymentImageNotFound();
            case DeployServiceUseCase.DeployServiceResult.DesiredStateFailure _,
                 DeployServiceUseCase.DeployServiceResult.DockerFailure _ ->
                    new HandleCiPipelineCompletedEventResult.DeploymentFailure();
        };
    }

    private HandleCiPipelineCompletedEventResult recordDeployment(
            ServiceName serviceName,
            OffsetDateTime now
    ) {
        return switch (recordCiDeploymentPort.recordCiDeployment(serviceName, now)) {
            case RecordCiDeploymentPort.RecordCiDeploymentResult.Recorded _ ->
                    new HandleCiPipelineCompletedEventResult.Deployed();
            case RecordCiDeploymentPort.RecordCiDeploymentResult.ServiceNotFound _ ->
                    new HandleCiPipelineCompletedEventResult.RecordDeploymentFailure();
            case RecordCiDeploymentPort.RecordCiDeploymentResult.Failure _ ->
                    new HandleCiPipelineCompletedEventResult.RecordDeploymentFailure();
        };
    }
}
