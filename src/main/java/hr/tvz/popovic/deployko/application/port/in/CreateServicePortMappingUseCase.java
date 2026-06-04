package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface CreateServicePortMappingUseCase {

    CreateServicePortMappingResult createServicePortMapping(CreateServicePortMappingCommand command);

    record CreateServicePortMappingCommand(ServiceName serviceName, Port hostPort, Port containerPort) {
    }

    sealed interface CreateServicePortMappingResult
            permits CreateServicePortMappingResult.Success, CreateServicePortMappingResult.ServiceNotFound,
            CreateServicePortMappingResult.AlreadyExists, CreateServicePortMappingResult.Failure {

        record Success() implements CreateServicePortMappingResult {
        }

        record ServiceNotFound() implements CreateServicePortMappingResult {
        }

        record AlreadyExists() implements CreateServicePortMappingResult {
        }

        record Failure() implements CreateServicePortMappingResult {
        }
    }
}
