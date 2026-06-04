package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.PortMappings;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface GetServicePortMappingsUseCase {

    GetServicePortMappingsResult getServicePortMappings(GetServicePortMappingsCommand command);

    record GetServicePortMappingsCommand(ServiceName serviceName) {
    }

    sealed interface GetServicePortMappingsResult
            permits GetServicePortMappingsResult.Success, GetServicePortMappingsResult.NotFound,
            GetServicePortMappingsResult.Failure {

        record Success(PortMappings portMappings) implements GetServicePortMappingsResult {
        }

        record NotFound() implements GetServicePortMappingsResult {
        }

        record Failure() implements GetServicePortMappingsResult {
        }
    }
}
