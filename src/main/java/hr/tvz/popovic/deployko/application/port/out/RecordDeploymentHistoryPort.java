package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface RecordDeploymentHistoryPort {

    RecordDeploymentHistoryResult recordDeployment(ServiceName serviceName, ImageVersion imageVersion);

    sealed interface RecordDeploymentHistoryResult
            permits RecordDeploymentHistoryResult.Recorded, RecordDeploymentHistoryResult.ServiceNotFound,
            RecordDeploymentHistoryResult.Failure {

        record Recorded() implements RecordDeploymentHistoryResult {
        }

        record ServiceNotFound() implements RecordDeploymentHistoryResult {
        }

        record Failure() implements RecordDeploymentHistoryResult {
        }
    }
}
