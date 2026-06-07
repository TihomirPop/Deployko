package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentAttempt;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface GetLatestDeploymentUseCase {

    GetLatestDeploymentResult getLatestDeployment(GetLatestDeploymentCommand command);

    record GetLatestDeploymentCommand(ServiceName serviceName) {
    }

    sealed interface GetLatestDeploymentResult
            permits GetLatestDeploymentResult.Found, GetLatestDeploymentResult.NotDeployed,
            GetLatestDeploymentResult.ServiceNotFound, GetLatestDeploymentResult.Failure {

        record Found(DeploymentAttempt deploymentAttempt) implements GetLatestDeploymentResult {
        }

        record NotDeployed() implements GetLatestDeploymentResult {
        }

        record ServiceNotFound() implements GetLatestDeploymentResult {
        }

        record Failure() implements GetLatestDeploymentResult {
        }
    }
}
