package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.ImageCommitSha;
import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;

import java.util.Objects;

public interface ResolveDeploymentImagePort {

    ResolveDeploymentImageResult resolveDeploymentImage(ImageRepository imageRepository, ImageVersion imageVersion);

    sealed interface ResolveDeploymentImageResult
            permits ResolveDeploymentImageResult.Found, ResolveDeploymentImageResult.ImageNotFound,
            ResolveDeploymentImageResult.Failure {

        record Found(ImageCommitSha commitSha) implements ResolveDeploymentImageResult {

            public Found {
                Objects.requireNonNull(commitSha, "commitSha must not be null");
            }
        }

        record ImageNotFound() implements ResolveDeploymentImageResult {
        }

        record Failure() implements ResolveDeploymentImageResult {
        }
    }
}
