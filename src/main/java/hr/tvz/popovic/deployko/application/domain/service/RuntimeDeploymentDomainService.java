package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentStatus;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.ImageCommitSha;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.port.in.DeployServiceUseCase.DeployServiceCommand;
import hr.tvz.popovic.deployko.application.port.in.DeployServiceUseCase.DeployServiceResult;
import hr.tvz.popovic.deployko.application.port.out.DeployContainerPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceDefinitionPort;
import hr.tvz.popovic.deployko.application.port.out.RecordDeploymentHistoryPort;
import hr.tvz.popovic.deployko.application.port.out.ResolveDeploymentImagePort;
import hr.tvz.popovic.deployko.application.port.out.UpdateDeploymentStatusPort;
import hr.tvz.popovic.deployko.application.port.out.UpsertDesiredDeploymentPort;

import java.util.Objects;

public final class RuntimeDeploymentDomainService {

    private final FindServiceDefinitionPort findServiceDefinitionPort;
    private final ResolveDeploymentImagePort resolveDeploymentImagePort;
    private final RecordDeploymentHistoryPort recordDeploymentHistoryPort;
    private final UpsertDesiredDeploymentPort upsertDesiredDeploymentPort;
    private final DeployContainerPort deployContainerPort;
    private final UpdateDeploymentStatusPort updateDeploymentStatusPort;
    private final DeploymentMonitor deploymentMonitor;

    public RuntimeDeploymentDomainService(
            FindServiceDefinitionPort findServiceDefinitionPort,
            ResolveDeploymentImagePort resolveDeploymentImagePort,
            RecordDeploymentHistoryPort recordDeploymentHistoryPort,
            UpsertDesiredDeploymentPort upsertDesiredDeploymentPort,
            DeployContainerPort deployContainerPort,
            UpdateDeploymentStatusPort updateDeploymentStatusPort,
            DeploymentMonitor deploymentMonitor
    ) {
        this.findServiceDefinitionPort = Objects.requireNonNull(
                findServiceDefinitionPort,
                "findServiceDefinitionPort must not be null"
        );
        this.resolveDeploymentImagePort = Objects.requireNonNull(
                resolveDeploymentImagePort,
                "resolveDeploymentImagePort must not be null"
        );
        this.recordDeploymentHistoryPort = Objects.requireNonNull(
                recordDeploymentHistoryPort,
                "recordDeploymentHistoryPort must not be null"
        );
        this.upsertDesiredDeploymentPort = Objects.requireNonNull(
                upsertDesiredDeploymentPort,
                "upsertDesiredDeploymentPort must not be null"
        );
        this.deployContainerPort = Objects.requireNonNull(deployContainerPort, "deployContainerPort must not be null");
        this.updateDeploymentStatusPort = Objects.requireNonNull(
                updateDeploymentStatusPort,
                "updateDeploymentStatusPort must not be null"
        );
        this.deploymentMonitor = Objects.requireNonNull(deploymentMonitor, "deploymentMonitor must not be null");
    }

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

    private DeployServiceResult deployFoundService(Service service, ImageVersion imageVersion) {
        ImageCommitSha commitSha;
        switch (resolveDeploymentImagePort.resolveDeploymentImage(service.imageRepository(), imageVersion)) {
            case ResolveDeploymentImagePort.ResolveDeploymentImageResult.Found found -> {
                commitSha = found.commitSha();
            }
            case ResolveDeploymentImagePort.ResolveDeploymentImageResult.ImageNotFound _ -> {
                return new DeployServiceResult.ImageNotFound();
            }
            case ResolveDeploymentImagePort.ResolveDeploymentImageResult.Failure _ -> {
                return new DeployServiceResult.DockerFailure();
            }
        }

        DeploymentId deploymentId;
        switch (recordDeploymentHistoryPort.recordDeployment(service.name(), imageVersion, commitSha)) {
            case RecordDeploymentHistoryPort.RecordDeploymentHistoryResult.Recorded recorded -> {
                deploymentId = recorded.deploymentId();
            }
            case RecordDeploymentHistoryPort.RecordDeploymentHistoryResult.ServiceNotFound _ -> {
                return new DeployServiceResult.ServiceNotFound();
            }
            case RecordDeploymentHistoryPort.RecordDeploymentHistoryResult.Failure _ -> {
                return new DeployServiceResult.DesiredStateFailure();
            }
        }

        DesiredDeployment desiredDeployment = new DesiredDeployment(
                service.name(),
                service.imageRepository(),
                imageVersion,
                service.runtimeConfiguration(),
                DesiredDeploymentState.RUNNING
        );

        DeployServiceResult deployServiceResult = deploy(desiredDeployment, deploymentId);
        if (deployServiceResult instanceof DeployServiceResult.Success) {
            deploymentMonitor.monitorDeployment(desiredDeployment, deploymentId);
        } else {
            updateDeploymentStatusPort.updateStatus(deploymentId, DeploymentStatus.FAILURE);
        }
        return deployServiceResult;
    }

    private DeployServiceResult deploy(DesiredDeployment desiredDeployment, DeploymentId deploymentId) {
        return switch (upsertDesiredDeploymentPort.upsert(desiredDeployment)) {
            case UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success _ ->
                    switch (deployContainerPort.deploy(desiredDeployment, deploymentId)) {
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
