package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;

public interface UpsertDesiredDeploymentPort {

    UpsertDesiredDeploymentResult upsert(DesiredDeployment desiredDeployment);

    sealed interface UpsertDesiredDeploymentResult
            permits UpsertDesiredDeploymentResult.Success, UpsertDesiredDeploymentResult.ServiceNotFound,
            UpsertDesiredDeploymentResult.Failure {

        record Success() implements UpsertDesiredDeploymentResult {
        }

        record ServiceNotFound() implements UpsertDesiredDeploymentResult {
        }

        record Failure() implements UpsertDesiredDeploymentResult {
        }
    }
}
