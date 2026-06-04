package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface DeleteServicePortMappingUseCase {

    DeleteServicePortMappingResult deleteServicePortMapping(DeleteServicePortMappingCommand command);

    record DeleteServicePortMappingCommand(ServiceName serviceName, Port hostPort) {
    }

    sealed interface DeleteServicePortMappingResult
            permits DeleteServicePortMappingResult.Success, DeleteServicePortMappingResult.ServiceNotFound,
            DeleteServicePortMappingResult.PortMappingNotFound, DeleteServicePortMappingResult.Failure {

        record Success() implements DeleteServicePortMappingResult {
        }

        record ServiceNotFound() implements DeleteServicePortMappingResult {
        }

        record PortMappingNotFound() implements DeleteServicePortMappingResult {
        }

        record Failure() implements DeleteServicePortMappingResult {
        }
    }
}
