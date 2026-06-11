package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentAttempt;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface GetDeploymentHistoryUseCase {

    GetDeploymentHistoryResult getDeploymentHistory(GetDeploymentHistoryCommand command);

    record GetDeploymentHistoryCommand(ServiceName serviceName, Optional<OffsetDateTime> since) {
    }

    sealed interface GetDeploymentHistoryResult
            permits GetDeploymentHistoryResult.Found, GetDeploymentHistoryResult.ServiceNotFound,
            GetDeploymentHistoryResult.Failure {

        record Found(List<DeploymentAttempt> deploymentAttempts) implements GetDeploymentHistoryResult {
        }

        record ServiceNotFound() implements GetDeploymentHistoryResult {
        }

        record Failure() implements GetDeploymentHistoryResult {
        }
    }
}
