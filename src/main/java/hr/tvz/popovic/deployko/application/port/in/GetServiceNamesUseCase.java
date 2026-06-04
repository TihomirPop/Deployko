package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import java.util.List;

public interface GetServiceNamesUseCase {

    GetServiceNamesResult getServiceNames();

    sealed interface GetServiceNamesResult permits GetServiceNamesResult.Success, GetServiceNamesResult.Failure {

        record Success(List<ServiceName> serviceNames) implements GetServiceNamesResult {
        }

        record Failure() implements GetServiceNamesResult {
        }
    }
}
