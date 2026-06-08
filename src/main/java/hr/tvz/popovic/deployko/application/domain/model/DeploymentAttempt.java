package hr.tvz.popovic.deployko.application.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;

public record DeploymentAttempt(
        DeploymentId deploymentId,
        ImageVersion imageVersion,
        ImageCommitSha commitSha,
        DeploymentStatus status,
        OffsetDateTime recordedAt
) {

    public DeploymentAttempt {
        Objects.requireNonNull(deploymentId, "deploymentId must not be null");
        Objects.requireNonNull(imageVersion, "imageVersion must not be null");
        Objects.requireNonNull(commitSha, "commitSha must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
    }
}
