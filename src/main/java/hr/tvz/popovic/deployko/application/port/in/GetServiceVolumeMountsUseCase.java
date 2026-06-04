package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMounts;

public interface GetServiceVolumeMountsUseCase {

    GetServiceVolumeMountsResult getServiceVolumeMounts(GetServiceVolumeMountsCommand command);

    record GetServiceVolumeMountsCommand(ServiceName serviceName) {
    }

    sealed interface GetServiceVolumeMountsResult
            permits GetServiceVolumeMountsResult.Success, GetServiceVolumeMountsResult.NotFound,
            GetServiceVolumeMountsResult.Failure {

        record Success(VolumeMounts volumeMounts) implements GetServiceVolumeMountsResult {
        }

        record NotFound() implements GetServiceVolumeMountsResult {
        }

        record Failure() implements GetServiceVolumeMountsResult {
        }
    }
}
