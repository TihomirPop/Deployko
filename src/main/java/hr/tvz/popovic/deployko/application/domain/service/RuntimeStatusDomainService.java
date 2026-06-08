package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.ActualDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.ServiceRuntimeStatus;
import hr.tvz.popovic.deployko.application.port.in.GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusCommand;
import hr.tvz.popovic.deployko.application.port.in.GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult;
import hr.tvz.popovic.deployko.application.port.out.FindActualDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.FindDesiredDeploymentStatePort;

import java.util.Objects;
import java.util.Optional;

public final class RuntimeStatusDomainService {

    private final FindDesiredDeploymentStatePort findDesiredDeploymentStatePort;
    private final FindActualDeploymentStatePort findActualDeploymentStatePort;

    public RuntimeStatusDomainService(
            FindDesiredDeploymentStatePort findDesiredDeploymentStatePort,
            FindActualDeploymentStatePort findActualDeploymentStatePort
    ) {
        this.findDesiredDeploymentStatePort = Objects.requireNonNull(
                findDesiredDeploymentStatePort,
                "findDesiredDeploymentStatePort must not be null"
        );
        this.findActualDeploymentStatePort = Objects.requireNonNull(
                findActualDeploymentStatePort,
                "findActualDeploymentStatePort must not be null"
        );
    }

    public GetServiceRuntimeStatusResult getServiceRuntimeStatus(GetServiceRuntimeStatusCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (findDesiredDeploymentStatePort.findDesiredState(command.serviceName())) {
            case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Found found ->
                    switch (findStatus(command.serviceName(), Optional.of(found.desiredState()))) {
                        case RuntimeStatusResult.Success success ->
                                new GetServiceRuntimeStatusResult.Success(success.status());
                        case RuntimeStatusResult.DockerFailure _ -> new GetServiceRuntimeStatusResult.DockerFailure();
                    };
            case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.NotDeployed _ ->
                    switch (findStatus(command.serviceName(), Optional.empty())) {
                        case RuntimeStatusResult.Success success ->
                                new GetServiceRuntimeStatusResult.Success(success.status());
                        case RuntimeStatusResult.DockerFailure _ -> new GetServiceRuntimeStatusResult.DockerFailure();
                    };
            case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.ServiceNotFound _ ->
                    new GetServiceRuntimeStatusResult.ServiceNotFound();
            case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Failure _ ->
                    new GetServiceRuntimeStatusResult.DesiredStateFailure();
        };
    }

    RuntimeStatusResult findStatus(
            ServiceName serviceName,
            Optional<DesiredDeploymentState> desiredState
    ) {
        return switch (findActualDeploymentStatePort.findActualState(serviceName)) {
            case FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found found ->
                    new RuntimeStatusResult.Success(desiredState
                            .map(state -> statusFor(state, found.actualState()))
                            .orElseGet(() -> statusWithoutDesiredDeployment(found.actualState())));
            case FindActualDeploymentStatePort.FindActualDeploymentStateResult.DuplicateManagedContainers _ ->
                    new RuntimeStatusResult.Success(ServiceRuntimeStatus.DUPLICATE_MANAGED_CONTAINERS);
            case FindActualDeploymentStatePort.FindActualDeploymentStateResult.Failure _ ->
                    new RuntimeStatusResult.DockerFailure();
        };
    }

    sealed interface RuntimeStatusResult permits RuntimeStatusResult.Success, RuntimeStatusResult.DockerFailure {

        record Success(ServiceRuntimeStatus status) implements RuntimeStatusResult {
        }

        record DockerFailure() implements RuntimeStatusResult {
        }
    }

    private static ServiceRuntimeStatus statusFor(
            DesiredDeploymentState desiredState,
            ActualDeploymentState actualState
    ) {
        return switch (desiredState) {
            case RUNNING -> switch (actualState) {
                case RUNNING -> ServiceRuntimeStatus.RUNNING;
                case STOPPED -> ServiceRuntimeStatus.EXPECTED_RUNNING_BUT_STOPPED;
                case MISSING -> ServiceRuntimeStatus.EXPECTED_RUNNING_BUT_MISSING;
            };
            case STOPPED -> switch (actualState) {
                case RUNNING -> ServiceRuntimeStatus.EXPECTED_STOPPED_BUT_RUNNING;
                case STOPPED -> ServiceRuntimeStatus.STOPPED;
                case MISSING -> ServiceRuntimeStatus.EXPECTED_STOPPED_BUT_MISSING;
            };
        };
    }

    private static ServiceRuntimeStatus statusWithoutDesiredDeployment(ActualDeploymentState actualState) {
        return switch (actualState) {
            case RUNNING -> ServiceRuntimeStatus.ORPHANED_RUNNING;
            case STOPPED -> ServiceRuntimeStatus.ORPHANED_STOPPED;
            case MISSING -> ServiceRuntimeStatus.NOT_DEPLOYED;
        };
    }
}
