package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;

public interface UpdateServiceVolumeMountPort {

    UpdateServiceVolumeMountResult updateVolumeMount(ServiceName serviceName, VolumeMount volumeMount);

    sealed interface UpdateServiceVolumeMountResult
            permits UpdateServiceVolumeMountResult.Updated, UpdateServiceVolumeMountResult.ServiceNotFound,
            UpdateServiceVolumeMountResult.VolumeMountNotFound, UpdateServiceVolumeMountResult.Failure {

        record Updated() implements UpdateServiceVolumeMountResult {
        }

        record ServiceNotFound() implements UpdateServiceVolumeMountResult {
        }

        record VolumeMountNotFound() implements UpdateServiceVolumeMountResult {
        }

        record Failure() implements UpdateServiceVolumeMountResult {
        }
    }
}
