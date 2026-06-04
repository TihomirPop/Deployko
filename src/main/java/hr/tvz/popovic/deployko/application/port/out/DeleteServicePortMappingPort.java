package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface DeleteServicePortMappingPort {

    DeleteServicePortMappingResult deletePortMapping(ServiceName serviceName, Port hostPort);

    sealed interface DeleteServicePortMappingResult
            permits DeleteServicePortMappingResult.Deleted, DeleteServicePortMappingResult.ServiceNotFound,
            DeleteServicePortMappingResult.PortMappingNotFound, DeleteServicePortMappingResult.Failure {

        record Deleted() implements DeleteServicePortMappingResult {
        }

        record ServiceNotFound() implements DeleteServicePortMappingResult {
        }

        record PortMappingNotFound() implements DeleteServicePortMappingResult {
        }

        record Failure() implements DeleteServicePortMappingResult {
        }
    }
}
