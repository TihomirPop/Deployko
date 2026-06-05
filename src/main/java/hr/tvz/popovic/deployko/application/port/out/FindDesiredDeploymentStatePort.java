package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface FindDesiredDeploymentStatePort {

    FindDesiredDeploymentStateResult findDesiredState(ServiceName serviceName);

    sealed interface FindDesiredDeploymentStateResult
            permits FindDesiredDeploymentStateResult.Found, FindDesiredDeploymentStateResult.ServiceNotFound,
            FindDesiredDeploymentStateResult.NotDeployed, FindDesiredDeploymentStateResult.Failure {

        record Found(DesiredDeploymentState desiredState) implements FindDesiredDeploymentStateResult {
        }

        record ServiceNotFound() implements FindDesiredDeploymentStateResult {
        }

        record NotDeployed() implements FindDesiredDeploymentStateResult {
        }

        record Failure() implements FindDesiredDeploymentStateResult {
        }
    }
}
