package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.ServiceRuntimeStatus;

public interface GetServiceRuntimeStatusUseCase {

    GetServiceRuntimeStatusResult getServiceRuntimeStatus(GetServiceRuntimeStatusCommand command);

    record GetServiceRuntimeStatusCommand(ServiceName serviceName) {
    }

    sealed interface GetServiceRuntimeStatusResult
            permits GetServiceRuntimeStatusResult.Success, GetServiceRuntimeStatusResult.ServiceNotFound,
            GetServiceRuntimeStatusResult.DesiredStateFailure, GetServiceRuntimeStatusResult.DockerFailure {

        record Success(ServiceRuntimeStatus status) implements GetServiceRuntimeStatusResult {
        }

        record ServiceNotFound() implements GetServiceRuntimeStatusResult {
        }

        record DesiredStateFailure() implements GetServiceRuntimeStatusResult {
        }

        record DockerFailure() implements GetServiceRuntimeStatusResult {
        }
    }
}
