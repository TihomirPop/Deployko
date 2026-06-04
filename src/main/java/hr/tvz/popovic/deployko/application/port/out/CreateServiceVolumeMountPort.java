package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;

public interface CreateServiceVolumeMountPort {

    CreateServiceVolumeMountResult createVolumeMount(ServiceName serviceName, VolumeMount volumeMount);

    sealed interface CreateServiceVolumeMountResult
            permits CreateServiceVolumeMountResult.Created, CreateServiceVolumeMountResult.ServiceNotFound,
            CreateServiceVolumeMountResult.AlreadyExists, CreateServiceVolumeMountResult.Failure {

        record Created() implements CreateServiceVolumeMountResult {
        }

        record ServiceNotFound() implements CreateServiceVolumeMountResult {
        }

        record AlreadyExists() implements CreateServiceVolumeMountResult {
        }

        record Failure() implements CreateServiceVolumeMountResult {
        }
    }
}
