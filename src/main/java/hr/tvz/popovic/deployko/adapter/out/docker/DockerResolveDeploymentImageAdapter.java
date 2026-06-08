package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import hr.tvz.popovic.deployko.application.domain.model.ImageCommitSha;
import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.port.out.ResolveDeploymentImagePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

public final class DockerResolveDeploymentImageAdapter implements ResolveDeploymentImagePort {

    private static final Logger log = LoggerFactory.getLogger(DockerResolveDeploymentImageAdapter.class);
    private static final String OCI_REVISION_LABEL = "org.opencontainers.image.revision";

    private final DockerJavaImageClient dockerImageClient;

    public DockerResolveDeploymentImageAdapter(DockerClient dockerClient) {
        this.dockerImageClient = new DockerJavaImageClient(dockerClient);
    }

    @Override
    public ResolveDeploymentImageResult resolveDeploymentImage(
            ImageRepository imageRepository,
            ImageVersion imageVersion
    ) {
        Objects.requireNonNull(imageRepository, "imageRepository must not be null");
        Objects.requireNonNull(imageVersion, "imageVersion must not be null");

        String imageReference = imageRepository.value() + ":" + imageVersion.value();
        try {
            dockerImageClient.pullImage(imageReference);
            return new ResolveDeploymentImageResult.Found(commitShaFrom(dockerImageClient.imageLabels(imageReference)));
        } catch (NotFoundException _) {
            return new ResolveDeploymentImageResult.ImageNotFound();
        } catch (DockerClientException exception) {
            if (isMissingImagePullFailure(exception)) {
                return new ResolveDeploymentImageResult.ImageNotFound();
            }
            log.error("Exception occurred while trying to resolve docker image {}", imageReference, exception);
            return new ResolveDeploymentImageResult.Failure();
        } catch (DockerException exception) {
            log.error("Exception occurred while trying to resolve docker image {}", imageReference, exception);
            return new ResolveDeploymentImageResult.Failure();
        }
    }

    private static ImageCommitSha commitShaFrom(Map<String, String> labels) {
        String revision = labels.get(OCI_REVISION_LABEL);
        if (revision == null || revision.isBlank()) {
            return new ImageCommitSha.Unknown();
        }
        return new ImageCommitSha.Known(revision);
    }

    private static boolean isMissingImagePullFailure(DockerClientException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }

        String normalizedMessage = message.toLowerCase();
        return normalizedMessage.contains("manifest unknown")
                || normalizedMessage.contains("not found")
                || normalizedMessage.contains("no such image");
    }
}
