package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.ActualDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

import java.util.Objects;

public interface FindActualDeploymentStatePort {

    FindActualDeploymentStateResult findActualState(ServiceName serviceName);

    sealed interface FindActualDeploymentStateResult
            permits FindActualDeploymentStateResult.Found, FindActualDeploymentStateResult.DuplicateManagedContainers,
            FindActualDeploymentStateResult.Failure {

        record Found(ActualDeploymentState actualState, int restartCount) implements FindActualDeploymentStateResult {

            public Found(ActualDeploymentState actualState) {
                this(actualState, 0);
            }

            public Found {
                Objects.requireNonNull(actualState, "actualState must not be null");
                if (restartCount < 0) {
                    throw new IllegalArgumentException("restartCount must not be negative");
                }
            }
        }

        record DuplicateManagedContainers() implements FindActualDeploymentStateResult {
        }

        record Failure() implements FindActualDeploymentStateResult {
        }
    }
}
