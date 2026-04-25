package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface ServiceManagementUseCase {

    CreateServiceResult createService(CreateServiceCommand command);

    DeleteServiceResult deleteService(DeleteServiceCommand command);

    record CreateServiceCommand(ServiceName serviceName, ImageRepository imageRepository) {
    }

    record DeleteServiceCommand(ServiceName serviceName) {
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

    sealed interface DeleteServiceResult
            permits DeleteServiceResult.Success, DeleteServiceResult.NotFound, DeleteServiceResult.Failure {

        record Success() implements DeleteServiceResult {
        }

        record NotFound() implements DeleteServiceResult {
        }

        record Failure() implements DeleteServiceResult {
        }
    }
}
