package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

import java.time.OffsetDateTime;

public interface FindLastCiDeploymentPort {

    FindLastCiDeploymentResult findLastCiDeployment(ServiceName serviceName);

    sealed interface FindLastCiDeploymentResult
            permits FindLastCiDeploymentResult.Found, FindLastCiDeploymentResult.NotDeployed,
            FindLastCiDeploymentResult.ServiceNotFound, FindLastCiDeploymentResult.Failure {

        record Found(OffsetDateTime deployedAt) implements FindLastCiDeploymentResult {
        }

        record NotDeployed() implements FindLastCiDeploymentResult {
        }

        record ServiceNotFound() implements FindLastCiDeploymentResult {
        }

        record Failure() implements FindLastCiDeploymentResult {
        }
    }
}
