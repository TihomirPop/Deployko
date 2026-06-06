package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface DeleteDesiredDeploymentPort {

    DeleteDesiredDeploymentResult delete(ServiceName serviceName);

    sealed interface DeleteDesiredDeploymentResult
            permits DeleteDesiredDeploymentResult.Deleted, DeleteDesiredDeploymentResult.ServiceNotFound,
            DeleteDesiredDeploymentResult.NotDeployed, DeleteDesiredDeploymentResult.Failure {

        record Deleted() implements DeleteDesiredDeploymentResult {
        }

        record ServiceNotFound() implements DeleteDesiredDeploymentResult {
        }

        record NotDeployed() implements DeleteDesiredDeploymentResult {
        }

        record Failure() implements DeleteDesiredDeploymentResult {
        }
    }
}
