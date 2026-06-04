package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface CreateServiceEnvironmentVariablePort {

    CreateServiceEnvironmentVariableResult createEnvironmentVariable(
            ServiceName serviceName,
            EnvironmentVariables.Key key,
            EnvironmentVariables.Value value
    );

    sealed interface CreateServiceEnvironmentVariableResult
            permits CreateServiceEnvironmentVariableResult.Created, CreateServiceEnvironmentVariableResult.ServiceNotFound,
            CreateServiceEnvironmentVariableResult.AlreadyExists, CreateServiceEnvironmentVariableResult.Failure {

        record Created() implements CreateServiceEnvironmentVariableResult {
        }

        record ServiceNotFound() implements CreateServiceEnvironmentVariableResult {
        }

        record AlreadyExists() implements CreateServiceEnvironmentVariableResult {
        }

        record Failure() implements CreateServiceEnvironmentVariableResult {
        }
    }
}
