package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface CreateServiceEnvironmentVariableUseCase {

    CreateServiceEnvironmentVariableResult createServiceEnvironmentVariable(
            CreateServiceEnvironmentVariableCommand command
    );

    record CreateServiceEnvironmentVariableCommand(
            ServiceName serviceName,
            EnvironmentVariables.Key key,
            EnvironmentVariables.Value value
    ) {
    }

    sealed interface CreateServiceEnvironmentVariableResult
            permits CreateServiceEnvironmentVariableResult.Success, CreateServiceEnvironmentVariableResult.ServiceNotFound,
            CreateServiceEnvironmentVariableResult.AlreadyExists, CreateServiceEnvironmentVariableResult.Failure {

        record Success() implements CreateServiceEnvironmentVariableResult {
        }

        record ServiceNotFound() implements CreateServiceEnvironmentVariableResult {
        }

        record AlreadyExists() implements CreateServiceEnvironmentVariableResult {
        }

        record Failure() implements CreateServiceEnvironmentVariableResult {
        }
    }
}
