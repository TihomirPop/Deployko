package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;

public interface DeleteServiceVolumeMountUseCase {

    DeleteServiceVolumeMountResult deleteServiceVolumeMount(DeleteServiceVolumeMountCommand command);

    record DeleteServiceVolumeMountCommand(ServiceName serviceName, VolumeMount.Target target) {
    }

    sealed interface DeleteServiceVolumeMountResult
            permits DeleteServiceVolumeMountResult.Success, DeleteServiceVolumeMountResult.ServiceNotFound,
            DeleteServiceVolumeMountResult.VolumeMountNotFound, DeleteServiceVolumeMountResult.Failure {

        record Success() implements DeleteServiceVolumeMountResult {
        }

        record ServiceNotFound() implements DeleteServiceVolumeMountResult {
        }

        record VolumeMountNotFound() implements DeleteServiceVolumeMountResult {
        }

        record Failure() implements DeleteServiceVolumeMountResult {
        }
    }
}
