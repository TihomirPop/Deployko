package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface StartServiceUseCase {

    StartServiceResult startService(StartServiceCommand command);

    record StartServiceCommand(ServiceName serviceName) {
    }

    sealed interface StartServiceResult
            permits StartServiceResult.Success, StartServiceResult.ServiceNotFound, StartServiceResult.NotDeployed,
            StartServiceResult.DesiredStateFailure, StartServiceResult.DockerFailure, StartServiceResult.Drift {

        record Success() implements StartServiceResult {
        }

        record ServiceNotFound() implements StartServiceResult {
        }

        record NotDeployed() implements StartServiceResult {
        }

        record DesiredStateFailure() implements StartServiceResult {
        }

        record DockerFailure() implements StartServiceResult {
        }

        record Drift() implements StartServiceResult {
        }
    }
}
