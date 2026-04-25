package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface DeleteServiceByNamePort {

    DeleteServiceByNameResult deleteByName(ServiceName serviceName);

    sealed interface DeleteServiceByNameResult
            permits DeleteServiceByNameResult.Deleted, DeleteServiceByNameResult.NotFound, DeleteServiceByNameResult.Failure {

        record Deleted() implements DeleteServiceByNameResult {
        }

        record NotFound() implements DeleteServiceByNameResult {
        }

        record Failure() implements DeleteServiceByNameResult {
        }
    }
}
