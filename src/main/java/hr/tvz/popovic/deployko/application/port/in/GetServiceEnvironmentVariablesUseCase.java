package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface GetServiceEnvironmentVariablesUseCase {

    GetServiceEnvironmentVariablesResult getServiceEnvironmentVariables(GetServiceEnvironmentVariablesCommand command);

    record GetServiceEnvironmentVariablesCommand(ServiceName serviceName) {
    }

    sealed interface GetServiceEnvironmentVariablesResult
            permits GetServiceEnvironmentVariablesResult.Success, GetServiceEnvironmentVariablesResult.NotFound,
            GetServiceEnvironmentVariablesResult.Failure {

        record Success(EnvironmentVariables environmentVariables) implements GetServiceEnvironmentVariablesResult {
        }

        record NotFound() implements GetServiceEnvironmentVariablesResult {
        }

        record Failure() implements GetServiceEnvironmentVariablesResult {
        }
    }
}
