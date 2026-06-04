package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface DeleteServiceEnvironmentVariableUseCase {

    DeleteServiceEnvironmentVariableResult deleteServiceEnvironmentVariable(
            DeleteServiceEnvironmentVariableCommand command
    );

    record DeleteServiceEnvironmentVariableCommand(ServiceName serviceName, EnvironmentVariables.Key key) {
    }

    sealed interface DeleteServiceEnvironmentVariableResult
            permits DeleteServiceEnvironmentVariableResult.Success, DeleteServiceEnvironmentVariableResult.ServiceNotFound,
            DeleteServiceEnvironmentVariableResult.EnvironmentVariableNotFound, DeleteServiceEnvironmentVariableResult.Failure {

        record Success() implements DeleteServiceEnvironmentVariableResult {
        }

        record ServiceNotFound() implements DeleteServiceEnvironmentVariableResult {
        }

        record EnvironmentVariableNotFound() implements DeleteServiceEnvironmentVariableResult {
        }

        record Failure() implements DeleteServiceEnvironmentVariableResult {
        }
    }
}
