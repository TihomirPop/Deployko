package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface CreateServiceUseCase {

    CreateServiceResult createService(CreateServiceCommand command);

    record CreateServiceCommand(ServiceName serviceName, ImageRepository imageRepository) {
    }

    sealed interface CreateServiceResult
            permits CreateServiceResult.Success, CreateServiceResult.DuplicateServiceName, CreateServiceResult.Failure {

        record Success(Service service) implements CreateServiceResult {
        }

        record DuplicateServiceName() implements CreateServiceResult {
        }

        record Failure() implements CreateServiceResult {
        }
    }
}
