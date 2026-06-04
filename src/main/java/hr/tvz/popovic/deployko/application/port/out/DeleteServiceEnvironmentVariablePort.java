package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface DeleteServiceEnvironmentVariablePort {

    DeleteServiceEnvironmentVariableResult deleteEnvironmentVariable(
            ServiceName serviceName,
            EnvironmentVariables.Key key
    );

    sealed interface DeleteServiceEnvironmentVariableResult
            permits DeleteServiceEnvironmentVariableResult.Deleted, DeleteServiceEnvironmentVariableResult.ServiceNotFound,
            DeleteServiceEnvironmentVariableResult.EnvironmentVariableNotFound, DeleteServiceEnvironmentVariableResult.Failure {

        record Deleted() implements DeleteServiceEnvironmentVariableResult {
        }

        record ServiceNotFound() implements DeleteServiceEnvironmentVariableResult {
        }

        record EnvironmentVariableNotFound() implements DeleteServiceEnvironmentVariableResult {
        }

        record Failure() implements DeleteServiceEnvironmentVariableResult {
        }
    }
}
