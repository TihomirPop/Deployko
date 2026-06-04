package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;

public interface DeleteServiceVolumeMountPort {

    DeleteServiceVolumeMountResult deleteVolumeMount(ServiceName serviceName, VolumeMount.Target target);

    sealed interface DeleteServiceVolumeMountResult
            permits DeleteServiceVolumeMountResult.Deleted, DeleteServiceVolumeMountResult.ServiceNotFound,
            DeleteServiceVolumeMountResult.VolumeMountNotFound, DeleteServiceVolumeMountResult.Failure {

        record Deleted() implements DeleteServiceVolumeMountResult {
        }

        record ServiceNotFound() implements DeleteServiceVolumeMountResult {
        }

        record VolumeMountNotFound() implements DeleteServiceVolumeMountResult {
        }

        record Failure() implements DeleteServiceVolumeMountResult {
        }
    }
}
