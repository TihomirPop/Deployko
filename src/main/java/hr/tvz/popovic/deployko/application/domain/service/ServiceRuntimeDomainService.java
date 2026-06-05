package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.ActualDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.ServiceRuntimeStatus;
import hr.tvz.popovic.deployko.application.port.in.DeployServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceRuntimeStatusUseCase;
import hr.tvz.popovic.deployko.application.port.in.StartServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.StopServiceUseCase;
import hr.tvz.popovic.deployko.application.port.out.DeployContainerPort;
import hr.tvz.popovic.deployko.application.port.out.FindActualDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.FindDesiredDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceDefinitionPort;
import hr.tvz.popovic.deployko.application.port.out.StartContainerPort;
import hr.tvz.popovic.deployko.application.port.out.StopContainerPort;
import hr.tvz.popovic.deployko.application.port.out.UpdateDesiredDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.UpsertDesiredDeploymentPort;

import java.util.Objects;

public final class ServiceRuntimeDomainService
        implements DeployServiceUseCase, StartServiceUseCase, StopServiceUseCase, GetServiceRuntimeStatusUseCase {

    private final FindServiceDefinitionPort findServiceDefinitionPort;
    private final UpsertDesiredDeploymentPort upsertDesiredDeploymentPort;
    private final UpdateDesiredDeploymentStatePort updateDesiredDeploymentStatePort;
    private final FindDesiredDeploymentStatePort findDesiredDeploymentStatePort;
    private final DeployContainerPort deployContainerPort;
    private final StartContainerPort startContainerPort;
    private final StopContainerPort stopContainerPort;
    private final FindActualDeploymentStatePort findActualDeploymentStatePort;

    public ServiceRuntimeDomainService(
            FindServiceDefinitionPort findServiceDefinitionPort,
            UpsertDesiredDeploymentPort upsertDesiredDeploymentPort,
            UpdateDesiredDeploymentStatePort updateDesiredDeploymentStatePort,
            FindDesiredDeploymentStatePort findDesiredDeploymentStatePort,
            DeployContainerPort deployContainerPort,
            StartContainerPort startContainerPort,
            StopContainerPort stopContainerPort,
            FindActualDeploymentStatePort findActualDeploymentStatePort
    ) {
        this.findServiceDefinitionPort = Objects.requireNonNull(
                findServiceDefinitionPort,
                "findServiceDefinitionPort must not be null"
        );
        this.upsertDesiredDeploymentPort = Objects.requireNonNull(
                upsertDesiredDeploymentPort,
                "upsertDesiredDeploymentPort must not be null"
        );
        this.updateDesiredDeploymentStatePort = Objects.requireNonNull(
                updateDesiredDeploymentStatePort,
                "updateDesiredDeploymentStatePort must not be null"
        );
        this.findDesiredDeploymentStatePort = Objects.requireNonNull(
                findDesiredDeploymentStatePort,
                "findDesiredDeploymentStatePort must not be null"
        );
        this.deployContainerPort = Objects.requireNonNull(
                deployContainerPort,
                "deployContainerPort must not be null"
        );
        this.startContainerPort = Objects.requireNonNull(
                startContainerPort,
                "startContainerPort must not be null"
        );
        this.stopContainerPort = Objects.requireNonNull(
                stopContainerPort,
                "stopContainerPort must not be null"
        );
        this.findActualDeploymentStatePort = Objects.requireNonNull(
                findActualDeploymentStatePort,
                "findActualDeploymentStatePort must not be null"
        );
    }

    @Override
    public DeployServiceResult deployService(DeployServiceCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (findServiceDefinitionPort.findByName(command.serviceName())) {
            case FindServiceDefinitionPort.FindServiceDefinitionResult.Found found ->
                    deployFoundService(found.service(), command.imageVersion());
            case FindServiceDefinitionPort.FindServiceDefinitionResult.NotFound _ ->
                    new DeployServiceResult.ServiceNotFound();
            case FindServiceDefinitionPort.FindServiceDefinitionResult.Failure _ ->
                    new DeployServiceResult.DesiredStateFailure();
        };
    }

    @Override
    public StartServiceResult startService(StartServiceCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (updateDesiredDeploymentStatePort.updateState(command.serviceName(), DesiredDeploymentState.RUNNING)) {
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.Success _ ->
                    switch (startContainerPort.start(command.serviceName())) {
                        case StartContainerPort.StartContainerResult.Success _ -> new StartServiceResult.Success();
                        case StartContainerPort.StartContainerResult.MissingContainer _ -> new StartServiceResult.NotDeployed();
                        case StartContainerPort.StartContainerResult.DuplicateManagedContainers _ -> new StartServiceResult.Drift();
                        case StartContainerPort.StartContainerResult.Failure _ -> new StartServiceResult.DockerFailure();
                    };
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.ServiceNotFound _ ->
                    new StartServiceResult.ServiceNotFound();
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.NotDeployed _ ->
                    new StartServiceResult.NotDeployed();
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.Failure _ ->
                    new StartServiceResult.DesiredStateFailure();
        };
    }

    @Override
    public StopServiceResult stopService(StopServiceCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (updateDesiredDeploymentStatePort.updateState(command.serviceName(), DesiredDeploymentState.STOPPED)) {
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.Success _ ->
                    switch (stopContainerPort.stop(command.serviceName())) {
                        case StopContainerPort.StopContainerResult.Success _ -> new StopServiceResult.Success();
                        case StopContainerPort.StopContainerResult.MissingContainer _ -> new StopServiceResult.NotDeployed();
                        case StopContainerPort.StopContainerResult.DuplicateManagedContainers _ -> new StopServiceResult.Drift();
                        case StopContainerPort.StopContainerResult.Failure _ -> new StopServiceResult.DockerFailure();
                    };
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.ServiceNotFound _ ->
                    new StopServiceResult.ServiceNotFound();
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.NotDeployed _ ->
                    new StopServiceResult.NotDeployed();
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.Failure _ ->
                    new StopServiceResult.DesiredStateFailure();
        };
    }

    @Override
    public GetServiceRuntimeStatusResult getServiceRuntimeStatus(GetServiceRuntimeStatusCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (findDesiredDeploymentStatePort.findDesiredState(command.serviceName())) {
            case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Found found ->
                    findStatusForDesiredState(command.serviceName(), found.desiredState());
            case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.NotDeployed _ ->
                    findStatusWithoutDesiredDeployment(command.serviceName());
            case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.ServiceNotFound _ ->
                    new GetServiceRuntimeStatusResult.ServiceNotFound();
            case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Failure _ ->
                    new GetServiceRuntimeStatusResult.DesiredStateFailure();
        };
    }

    private DeployServiceResult deployFoundService(Service service, ImageVersion imageVersion) {
        DesiredDeployment desiredDeployment = new DesiredDeployment(
                service.name(),
                service.imageRepository(),
                imageVersion,
                service.runtimeConfiguration(),
                DesiredDeploymentState.RUNNING
        );

        return switch (upsertDesiredDeploymentPort.upsert(desiredDeployment)) {
            case UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success _ ->
                    switch (deployContainerPort.deploy(desiredDeployment)) {
                        case DeployContainerPort.DeployContainerResult.Success _ -> new DeployServiceResult.Success();
                        case DeployContainerPort.DeployContainerResult.Failure _ ->
                                new DeployServiceResult.DockerFailure();
                    };
            case UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.ServiceNotFound _ ->
                    new DeployServiceResult.ServiceNotFound();
            case UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Failure _ ->
                    new DeployServiceResult.DesiredStateFailure();
        };
    }

    private GetServiceRuntimeStatusResult findStatusForDesiredState(
            ServiceName serviceName,
            DesiredDeploymentState desiredState
    ) {
        return switch (findActualDeploymentStatePort.findActualState(serviceName)) {
            case FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found found ->
                    new GetServiceRuntimeStatusResult.Success(statusFor(desiredState, found.actualState()));
            case FindActualDeploymentStatePort.FindActualDeploymentStateResult.DuplicateManagedContainers _ ->
                    new GetServiceRuntimeStatusResult.Success(ServiceRuntimeStatus.DUPLICATE_MANAGED_CONTAINERS);
            case FindActualDeploymentStatePort.FindActualDeploymentStateResult.Failure _ ->
                    new GetServiceRuntimeStatusResult.DockerFailure();
        };
    }

    private GetServiceRuntimeStatusResult findStatusWithoutDesiredDeployment(ServiceName serviceName) {
        return switch (findActualDeploymentStatePort.findActualState(serviceName)) {
            case FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found found ->
                    new GetServiceRuntimeStatusResult.Success(statusWithoutDesiredDeployment(found.actualState()));
            case FindActualDeploymentStatePort.FindActualDeploymentStateResult.DuplicateManagedContainers _ ->
                    new GetServiceRuntimeStatusResult.Success(ServiceRuntimeStatus.DUPLICATE_MANAGED_CONTAINERS);
            case FindActualDeploymentStatePort.FindActualDeploymentStateResult.Failure _ ->
                    new GetServiceRuntimeStatusResult.DockerFailure();
        };
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
