package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface UpdateServiceEnvironmentVariableUseCase {

    UpdateServiceEnvironmentVariableResult updateServiceEnvironmentVariable(
            UpdateServiceEnvironmentVariableCommand command
    );

    record UpdateServiceEnvironmentVariableCommand(
            ServiceName serviceName,
            EnvironmentVariables.Key key,
            EnvironmentVariables.Value value
    ) {
    }

    sealed interface UpdateServiceEnvironmentVariableResult
            permits UpdateServiceEnvironmentVariableResult.Success, UpdateServiceEnvironmentVariableResult.ServiceNotFound,
            UpdateServiceEnvironmentVariableResult.EnvironmentVariableNotFound, UpdateServiceEnvironmentVariableResult.Failure {

        record Success() implements UpdateServiceEnvironmentVariableResult {
        }

        record ServiceNotFound() implements UpdateServiceEnvironmentVariableResult {
        }

        record EnvironmentVariableNotFound() implements UpdateServiceEnvironmentVariableResult {
        }

        record Failure() implements UpdateServiceEnvironmentVariableResult {
        }
    }
}
