package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface ServiceDeploymentUseCase {

    DeployServiceResult deployService(DeployServiceCommand command);

    StartServiceResult startService(StartServiceCommand command);

    StopServiceResult stopService(StopServiceCommand command);

    record DeployServiceCommand(
            ServiceName serviceName,
            ImageVersion imageVersion
    ) {
    }

    record StartServiceCommand(ServiceName serviceName) {
    }

    record StopServiceCommand(ServiceName serviceName) {
    }

    sealed interface DeployServiceResult
            permits DeployServiceResult.Success, DeployServiceResult.ServiceNotFound,
            DeployServiceResult.DesiredStateFailure, DeployServiceResult.DockerFailure {

        record Success() implements DeployServiceResult {
        }

        record ServiceNotFound() implements DeployServiceResult {
        }

        record DesiredStateFailure() implements DeployServiceResult {
        }

        record DockerFailure() implements DeployServiceResult {
        }
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
