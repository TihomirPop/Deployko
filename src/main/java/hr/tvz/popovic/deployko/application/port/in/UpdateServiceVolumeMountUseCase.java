package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;

public interface UpdateServiceVolumeMountUseCase {

    UpdateServiceVolumeMountResult updateServiceVolumeMount(UpdateServiceVolumeMountCommand command);

    record UpdateServiceVolumeMountCommand(ServiceName serviceName, VolumeMount volumeMount) {
    }

    sealed interface UpdateServiceVolumeMountResult
            permits UpdateServiceVolumeMountResult.Success, UpdateServiceVolumeMountResult.ServiceNotFound,
            UpdateServiceVolumeMountResult.VolumeMountNotFound, UpdateServiceVolumeMountResult.Failure {

        record Success() implements UpdateServiceVolumeMountResult {
        }

        record ServiceNotFound() implements UpdateServiceVolumeMountResult {
        }

        record VolumeMountNotFound() implements UpdateServiceVolumeMountResult {
        }

        record Failure() implements UpdateServiceVolumeMountResult {
        }
    }
}
