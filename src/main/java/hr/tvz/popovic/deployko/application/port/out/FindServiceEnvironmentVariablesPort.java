package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface FindServiceEnvironmentVariablesPort {

    FindServiceEnvironmentVariablesResult findEnvironmentVariables(ServiceName serviceName);

    sealed interface FindServiceEnvironmentVariablesResult
            permits FindServiceEnvironmentVariablesResult.Found, FindServiceEnvironmentVariablesResult.ServiceNotFound,
            FindServiceEnvironmentVariablesResult.Failure {

        record Found(EnvironmentVariables environmentVariables) implements FindServiceEnvironmentVariablesResult {
        }

        record ServiceNotFound() implements FindServiceEnvironmentVariablesResult {
        }

        record Failure() implements FindServiceEnvironmentVariablesResult {
        }
    }
}
