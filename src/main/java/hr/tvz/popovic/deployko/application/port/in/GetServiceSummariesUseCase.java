package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.ServiceSummary;

import java.util.List;

public interface GetServiceSummariesUseCase {

    GetServiceSummariesResult getServiceSummaries();

    sealed interface GetServiceSummariesResult
            permits GetServiceSummariesResult.Success, GetServiceSummariesResult.Failure {

        record Success(List<ServiceSummary> services) implements GetServiceSummariesResult {
        }

        record Failure() implements GetServiceSummariesResult {
        }
    }
}
