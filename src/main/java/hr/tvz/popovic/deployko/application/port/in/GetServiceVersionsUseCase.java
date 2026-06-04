package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

import java.util.List;

public interface GetServiceVersionsUseCase {

    GetServiceVersionsResult getServiceVersions(GetServiceVersionsCommand command);

    record GetServiceVersionsCommand(ServiceName serviceName) {
    }

    sealed interface GetServiceVersionsResult
            permits GetServiceVersionsResult.Success, GetServiceVersionsResult.NotFound,
            GetServiceVersionsResult.Failure {

        record Success(List<ImageVersion> imageVersions) implements GetServiceVersionsResult {
        }

        record NotFound() implements GetServiceVersionsResult {
        }

        record Failure() implements GetServiceVersionsResult {
        }
    }
}
