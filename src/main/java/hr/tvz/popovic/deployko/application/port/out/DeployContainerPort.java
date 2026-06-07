package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;

public interface DeployContainerPort {

    DeployContainerResult deploy(DesiredDeployment desiredDeployment, DeploymentId deploymentId);

    sealed interface DeployContainerResult
            permits DeployContainerResult.Success, DeployContainerResult.Failure {

        record Success() implements DeployContainerResult {
        }

        record Failure() implements DeployContainerResult {
        }
    }
}
