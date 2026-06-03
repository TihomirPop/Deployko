package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface FindServiceDefinitionPort {

    FindServiceDefinitionResult findByName(ServiceName serviceName);

    sealed interface FindServiceDefinitionResult
            permits FindServiceDefinitionResult.Found, FindServiceDefinitionResult.NotFound,
            FindServiceDefinitionResult.Failure {

        record Found(Service service) implements FindServiceDefinitionResult {
        }

        record NotFound() implements FindServiceDefinitionResult {
        }

        record Failure() implements FindServiceDefinitionResult {
        }
    }
}
