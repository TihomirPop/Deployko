package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.ActualDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface FindActualDeploymentStatePort {

    FindActualDeploymentStateResult findActualState(ServiceName serviceName);

    sealed interface FindActualDeploymentStateResult
            permits FindActualDeploymentStateResult.Found, FindActualDeploymentStateResult.DuplicateManagedContainers,
            FindActualDeploymentStateResult.Failure {

        record Found(ActualDeploymentState actualState) implements FindActualDeploymentStateResult {
        }

        record DuplicateManagedContainers() implements FindActualDeploymentStateResult {
        }

        record Failure() implements FindActualDeploymentStateResult {
        }
    }
}
