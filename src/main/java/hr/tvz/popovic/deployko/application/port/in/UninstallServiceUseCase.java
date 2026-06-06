package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface UninstallServiceUseCase {

    UninstallServiceResult uninstallService(UninstallServiceCommand command);

    record UninstallServiceCommand(ServiceName serviceName) {
    }

    sealed interface UninstallServiceResult
            permits UninstallServiceResult.Success, UninstallServiceResult.ServiceNotFound,
            UninstallServiceResult.NotDeployed, UninstallServiceResult.DesiredStateFailure,
            UninstallServiceResult.DockerFailure, UninstallServiceResult.Drift {

        record Success() implements UninstallServiceResult {
        }

        record ServiceNotFound() implements UninstallServiceResult {
        }

        record NotDeployed() implements UninstallServiceResult {
        }

        record DesiredStateFailure() implements UninstallServiceResult {
        }

        record DockerFailure() implements UninstallServiceResult {
        }

        record Drift() implements UninstallServiceResult {
        }
    }
}
