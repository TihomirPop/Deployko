package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.port.in.GetDeploymentHistoryUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetLatestDeploymentUseCase;
import hr.tvz.popovic.deployko.application.port.out.FindDeploymentHistoryPort;
import hr.tvz.popovic.deployko.application.port.out.FindLatestDeploymentPort;

import java.util.Objects;

public final class DeploymentHistoryDomainService
        implements GetLatestDeploymentUseCase, GetDeploymentHistoryUseCase {

    private final FindLatestDeploymentPort findLatestDeploymentPort;
    private final FindDeploymentHistoryPort findDeploymentHistoryPort;

    public DeploymentHistoryDomainService(
            FindLatestDeploymentPort findLatestDeploymentPort,
            FindDeploymentHistoryPort findDeploymentHistoryPort
    ) {
        this.findLatestDeploymentPort = Objects.requireNonNull(
                findLatestDeploymentPort,
                "findLatestDeploymentPort must not be null"
        );
        this.findDeploymentHistoryPort = Objects.requireNonNull(
                findDeploymentHistoryPort,
                "findDeploymentHistoryPort must not be null"
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

    @Override
    public GetDeploymentHistoryResult getDeploymentHistory(GetDeploymentHistoryCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (findDeploymentHistoryPort.findDeploymentHistory(command.serviceName(), command.since())) {
            case FindDeploymentHistoryPort.FindDeploymentHistoryResult.Found found ->
                    new GetDeploymentHistoryResult.Found(found.deploymentAttempts());
            case FindDeploymentHistoryPort.FindDeploymentHistoryResult.ServiceNotFound _ ->
                    new GetDeploymentHistoryResult.ServiceNotFound();
            case FindDeploymentHistoryPort.FindDeploymentHistoryResult.Failure _ ->
                    new GetDeploymentHistoryResult.Failure();
        };
    }
}
