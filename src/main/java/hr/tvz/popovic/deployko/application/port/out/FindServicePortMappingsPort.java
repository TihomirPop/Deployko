package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.PortMappings;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface FindServicePortMappingsPort {

    FindServicePortMappingsResult findPortMappings(ServiceName serviceName);

    sealed interface FindServicePortMappingsResult
            permits FindServicePortMappingsResult.Found, FindServicePortMappingsResult.ServiceNotFound,
            FindServicePortMappingsResult.Failure {

        record Found(PortMappings portMappings) implements FindServicePortMappingsResult {
        }

        record ServiceNotFound() implements FindServicePortMappingsResult {
        }

        record Failure() implements FindServicePortMappingsResult {
        }
    }
}
