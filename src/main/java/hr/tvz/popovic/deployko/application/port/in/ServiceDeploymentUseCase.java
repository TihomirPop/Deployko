package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.RuntimeConfiguration;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface ServiceDeploymentUseCase {

    DeployServiceResult deployService(DeployServiceCommand command);

    record DeployServiceCommand(
            ServiceName serviceName,
            ImageVersion imageVersion,
            RuntimeConfiguration runtimeConfiguration
    ) {
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
}
