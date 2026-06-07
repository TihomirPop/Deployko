package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.port.in.GetLatestDeploymentUseCase;
import hr.tvz.popovic.deployko.application.port.out.FindLatestDeploymentPort;

import java.util.Objects;

public final class DeploymentHistoryDomainService implements GetLatestDeploymentUseCase {

    private final FindLatestDeploymentPort findLatestDeploymentPort;

    public DeploymentHistoryDomainService(FindLatestDeploymentPort findLatestDeploymentPort) {
        this.findLatestDeploymentPort = Objects.requireNonNull(
                findLatestDeploymentPort,
                "findLatestDeploymentPort must not be null"
        );
    }

    @Override
    public GetLatestDeploymentResult getLatestDeployment(GetLatestDeploymentCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (findLatestDeploymentPort.findLatestDeployment(command.serviceName())) {
            case FindLatestDeploymentPort.FindLatestDeploymentResult.Found found ->
                    new GetLatestDeploymentResult.Found(found.deploymentAttempt());
            case FindLatestDeploymentPort.FindLatestDeploymentResult.NotDeployed _ ->
                    new GetLatestDeploymentResult.NotDeployed();
            case FindLatestDeploymentPort.FindLatestDeploymentResult.ServiceNotFound _ ->
                    new GetLatestDeploymentResult.ServiceNotFound();
            case FindLatestDeploymentPort.FindLatestDeploymentResult.Failure _ ->
                    new GetLatestDeploymentResult.Failure();
        };
    }
}
