package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMounts;

public interface FindServiceVolumeMountsPort {

    FindServiceVolumeMountsResult findVolumeMounts(ServiceName serviceName);

    sealed interface FindServiceVolumeMountsResult
            permits FindServiceVolumeMountsResult.Found, FindServiceVolumeMountsResult.ServiceNotFound,
            FindServiceVolumeMountsResult.Failure {

        record Found(VolumeMounts volumeMounts) implements FindServiceVolumeMountsResult {
        }

        record ServiceNotFound() implements FindServiceVolumeMountsResult {
        }

        record Failure() implements FindServiceVolumeMountsResult {
        }
    }
}
