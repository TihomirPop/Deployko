package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.ImageCommitSha;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import java.util.Objects;

public interface RecordDeploymentHistoryPort {

    RecordDeploymentHistoryResult recordDeployment(
            ServiceName serviceName,
            ImageVersion imageVersion,
            ImageCommitSha commitSha
    );

    sealed interface RecordDeploymentHistoryResult
            permits RecordDeploymentHistoryResult.Recorded, RecordDeploymentHistoryResult.ServiceNotFound,
            RecordDeploymentHistoryResult.Failure {

        record Recorded(DeploymentId deploymentId) implements RecordDeploymentHistoryResult {

            public Recorded {
                Objects.requireNonNull(deploymentId, "deploymentId must not be null");
            }
        }

        record ServiceNotFound() implements RecordDeploymentHistoryResult {
        }

        record Failure() implements RecordDeploymentHistoryResult {
        }
    }
}
