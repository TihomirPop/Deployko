package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface UpdateDesiredDeploymentStatePort {

    UpdateDesiredDeploymentStateResult updateState(ServiceName serviceName, DesiredDeploymentState desiredState);

    sealed interface UpdateDesiredDeploymentStateResult
            permits UpdateDesiredDeploymentStateResult.Success, UpdateDesiredDeploymentStateResult.ServiceNotFound,
            UpdateDesiredDeploymentStateResult.NotDeployed, UpdateDesiredDeploymentStateResult.Failure {

        record Success() implements UpdateDesiredDeploymentStateResult {
        }

        record ServiceNotFound() implements UpdateDesiredDeploymentStateResult {
        }

        record NotDeployed() implements UpdateDesiredDeploymentStateResult {
        }

        record Failure() implements UpdateDesiredDeploymentStateResult {
        }
    }
}
