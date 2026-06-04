package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.port.in.ServiceDeploymentUseCase;
import hr.tvz.popovic.deployko.application.port.out.DeployContainerPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceDefinitionPort;
import hr.tvz.popovic.deployko.application.port.out.StartContainerPort;
import hr.tvz.popovic.deployko.application.port.out.StopContainerPort;
import hr.tvz.popovic.deployko.application.port.out.UpdateDesiredDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.UpsertDesiredDeploymentPort;

import java.util.Objects;

public final class ServiceDeploymentDomainService implements ServiceDeploymentUseCase {

    private final FindServiceDefinitionPort findServiceDefinitionPort;
    private final UpsertDesiredDeploymentPort upsertDesiredDeploymentPort;
    private final UpdateDesiredDeploymentStatePort updateDesiredDeploymentStatePort;
    private final DeployContainerPort deployContainerPort;
    private final StartContainerPort startContainerPort;
    private final StopContainerPort stopContainerPort;

    public ServiceDeploymentDomainService(
            FindServiceDefinitionPort findServiceDefinitionPort,
            UpsertDesiredDeploymentPort upsertDesiredDeploymentPort,
            UpdateDesiredDeploymentStatePort updateDesiredDeploymentStatePort,
            DeployContainerPort deployContainerPort,
            StartContainerPort startContainerPort,
            StopContainerPort stopContainerPort
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
}
