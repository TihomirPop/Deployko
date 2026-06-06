package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface DeleteServiceUseCase {

    DeleteServiceResult deleteService(DeleteServiceCommand command);

    record DeleteServiceCommand(ServiceName serviceName) {
    }

    sealed interface DeleteServiceResult
            permits DeleteServiceResult.Success, DeleteServiceResult.NotFound, DeleteServiceResult.DeploymentExists,
            DeleteServiceResult.Failure {

        record Success() implements DeleteServiceResult {
        }

        record NotFound() implements DeleteServiceResult {
        }

        record DeploymentExists() implements DeleteServiceResult {
        }

        record Failure() implements DeleteServiceResult {
        }
    }
}
