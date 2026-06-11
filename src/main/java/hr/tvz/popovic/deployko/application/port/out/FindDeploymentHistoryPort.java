package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentAttempt;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface FindDeploymentHistoryPort {

    FindDeploymentHistoryResult findDeploymentHistory(ServiceName serviceName, Optional<OffsetDateTime> since);

    sealed interface FindDeploymentHistoryResult
            permits FindDeploymentHistoryResult.Found, FindDeploymentHistoryResult.ServiceNotFound,
            FindDeploymentHistoryResult.Failure {

        record Found(List<DeploymentAttempt> deploymentAttempts) implements FindDeploymentHistoryResult {
        }

        record ServiceNotFound() implements FindDeploymentHistoryResult {
        }

        record Failure() implements FindDeploymentHistoryResult {
        }
    }
}
