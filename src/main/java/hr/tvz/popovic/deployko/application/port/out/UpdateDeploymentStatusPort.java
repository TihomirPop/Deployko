package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentStatus;

public interface UpdateDeploymentStatusPort {

    UpdateDeploymentStatusResult updateStatus(DeploymentId deploymentId, DeploymentStatus status);

    sealed interface UpdateDeploymentStatusResult
            permits UpdateDeploymentStatusResult.Success, UpdateDeploymentStatusResult.DeploymentNotFound,
            UpdateDeploymentStatusResult.Failure {

        record Success() implements UpdateDeploymentStatusResult {
        }

        record DeploymentNotFound() implements UpdateDeploymentStatusResult {
        }

        record Failure() implements UpdateDeploymentStatusResult {
        }
    }
}
