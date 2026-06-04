package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface StopServiceUseCase {

    StopServiceResult stopService(StopServiceCommand command);

    record StopServiceCommand(ServiceName serviceName) {
    }

    sealed interface StopServiceResult
            permits StopServiceResult.Success, StopServiceResult.ServiceNotFound, StopServiceResult.NotDeployed,
            StopServiceResult.DesiredStateFailure, StopServiceResult.DockerFailure, StopServiceResult.Drift {

        record Success() implements StopServiceResult {
        }

        record ServiceNotFound() implements StopServiceResult {
        }

        record NotDeployed() implements StopServiceResult {
        }

        record DesiredStateFailure() implements StopServiceResult {
        }

        record DockerFailure() implements StopServiceResult {
        }

        record Drift() implements StopServiceResult {
        }
    }
}
