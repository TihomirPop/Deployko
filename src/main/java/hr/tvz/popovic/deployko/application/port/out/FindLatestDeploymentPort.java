package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentAttempt;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface FindLatestDeploymentPort {

    FindLatestDeploymentResult findLatestDeployment(ServiceName serviceName);

    sealed interface FindLatestDeploymentResult
            permits FindLatestDeploymentResult.Found, FindLatestDeploymentResult.NotDeployed,
            FindLatestDeploymentResult.ServiceNotFound, FindLatestDeploymentResult.Failure {

        record Found(DeploymentAttempt deploymentAttempt) implements FindLatestDeploymentResult {
        }

        record NotDeployed() implements FindLatestDeploymentResult {
        }

        record ServiceNotFound() implements FindLatestDeploymentResult {
        }

        record Failure() implements FindLatestDeploymentResult {
        }
    }
}
