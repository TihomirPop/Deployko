package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.Service;

public interface CreateServicePort {

    CreateServicePortResult create(Service service);

    sealed interface CreateServicePortResult
            permits CreateServicePortResult.Success, CreateServicePortResult.AlreadyExists, CreateServicePortResult.Failure {

        record Success() implements CreateServicePortResult {
        }

        record AlreadyExists() implements CreateServicePortResult {
        }

        record Failure() implements CreateServicePortResult {
        }
    }
}
