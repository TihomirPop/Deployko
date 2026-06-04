package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;

public interface CreateServiceVolumeMountUseCase {

    CreateServiceVolumeMountResult createServiceVolumeMount(CreateServiceVolumeMountCommand command);

    record CreateServiceVolumeMountCommand(ServiceName serviceName, VolumeMount volumeMount) {
    }

    sealed interface CreateServiceVolumeMountResult
            permits CreateServiceVolumeMountResult.Success, CreateServiceVolumeMountResult.ServiceNotFound,
            CreateServiceVolumeMountResult.AlreadyExists, CreateServiceVolumeMountResult.Failure {

        record Success() implements CreateServiceVolumeMountResult {
        }

        record ServiceNotFound() implements CreateServiceVolumeMountResult {
        }

        record AlreadyExists() implements CreateServiceVolumeMountResult {
        }

        record Failure() implements CreateServiceVolumeMountResult {
        }
    }
}
