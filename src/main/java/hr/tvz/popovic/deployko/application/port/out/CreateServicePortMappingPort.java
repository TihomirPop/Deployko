package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface CreateServicePortMappingPort {

    CreateServicePortMappingResult createPortMapping(ServiceName serviceName, Port hostPort, Port containerPort);

    sealed interface CreateServicePortMappingResult
            permits CreateServicePortMappingResult.Created, CreateServicePortMappingResult.ServiceNotFound,
            CreateServicePortMappingResult.AlreadyExists, CreateServicePortMappingResult.Failure {

        record Created() implements CreateServicePortMappingResult {
        }

        record ServiceNotFound() implements CreateServicePortMappingResult {
        }

        record AlreadyExists() implements CreateServicePortMappingResult {
        }

        record Failure() implements CreateServicePortMappingResult {
        }
    }
}
