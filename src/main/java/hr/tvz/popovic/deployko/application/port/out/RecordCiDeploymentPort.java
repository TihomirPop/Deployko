package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

import java.time.OffsetDateTime;

public interface RecordCiDeploymentPort {

    RecordCiDeploymentResult recordCiDeployment(ServiceName serviceName, OffsetDateTime deployedAt);

    sealed interface RecordCiDeploymentResult
            permits RecordCiDeploymentResult.Recorded, RecordCiDeploymentResult.ServiceNotFound,
            RecordCiDeploymentResult.Failure {

        record Recorded() implements RecordCiDeploymentResult {
        }

        record ServiceNotFound() implements RecordCiDeploymentResult {
        }

        record Failure() implements RecordCiDeploymentResult {
        }
    }
}
