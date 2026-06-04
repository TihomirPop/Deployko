package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface UpdateServiceEnvironmentVariablePort {

    UpdateServiceEnvironmentVariableResult updateEnvironmentVariable(
            ServiceName serviceName,
            EnvironmentVariables.Key key,
            EnvironmentVariables.Value value
    );

    sealed interface UpdateServiceEnvironmentVariableResult
            permits UpdateServiceEnvironmentVariableResult.Updated, UpdateServiceEnvironmentVariableResult.ServiceNotFound,
            UpdateServiceEnvironmentVariableResult.EnvironmentVariableNotFound, UpdateServiceEnvironmentVariableResult.Failure {

        record Updated() implements UpdateServiceEnvironmentVariableResult {
        }

        record ServiceNotFound() implements UpdateServiceEnvironmentVariableResult {
        }

        record EnvironmentVariableNotFound() implements UpdateServiceEnvironmentVariableResult {
        }

        record Failure() implements UpdateServiceEnvironmentVariableResult {
        }
    }
}
