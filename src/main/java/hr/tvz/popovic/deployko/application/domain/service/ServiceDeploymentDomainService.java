package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.port.in.ServiceDeploymentUseCase;
import hr.tvz.popovic.deployko.application.port.out.DeployContainerPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceDefinitionPort;
import hr.tvz.popovic.deployko.application.port.out.UpsertDesiredDeploymentPort;

import java.util.Objects;

public final class ServiceDeploymentDomainService implements ServiceDeploymentUseCase {

    private final FindServiceDefinitionPort findServiceDefinitionPort;
    private final UpsertDesiredDeploymentPort upsertDesiredDeploymentPort;
    private final DeployContainerPort deployContainerPort;

    public ServiceDeploymentDomainService(
            FindServiceDefinitionPort findServiceDefinitionPort,
            UpsertDesiredDeploymentPort upsertDesiredDeploymentPort,
            DeployContainerPort deployContainerPort
    ) {
        this.findServiceDefinitionPort = Objects.requireNonNull(
                findServiceDefinitionPort,
                "findServiceDefinitionPort must not be null"
        );
        this.upsertDesiredDeploymentPort = Objects.requireNonNull(
                upsertDesiredDeploymentPort,
                "upsertDesiredDeploymentPort must not be null"
        );
        this.deployContainerPort = Objects.requireNonNull(
                deployContainerPort,
                "deployContainerPort must not be null"
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
        throw new UnsupportedOperationException("start service is not implemented yet");
    }

    @Override
    public StopServiceResult stopService(StopServiceCommand command) {
        throw new UnsupportedOperationException("stop service is not implemented yet");
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
