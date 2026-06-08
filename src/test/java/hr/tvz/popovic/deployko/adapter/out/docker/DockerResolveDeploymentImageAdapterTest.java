package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.NotFoundException;
import hr.tvz.popovic.deployko.application.domain.model.ImageCommitSha;
import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.port.out.ResolveDeploymentImagePort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DockerResolveDeploymentImageAdapterTest {

    private static final ImageRepository IMAGE_REPOSITORY = new ImageRepository("ghcr.io/deployko/api");
    private static final ImageVersion IMAGE_VERSION = new ImageVersion("1.0.0");

    private final FakeDockerImageClient dockerImageClient = new FakeDockerImageClient();
    private final DockerResolveDeploymentImageAdapter adapter = new DockerResolveDeploymentImageAdapter(dockerImageClient);

    @Test
    void pulls_image_and_returns_known_commit_sha_from_revision_label() {
        dockerImageClient.labels.put("org.opencontainers.image.revision", "f5a1c2d");

        ResolveDeploymentImagePort.ResolveDeploymentImageResult result =
                adapter.resolveDeploymentImage(IMAGE_REPOSITORY, IMAGE_VERSION);

        assertThat(result).isEqualTo(new ResolveDeploymentImagePort.ResolveDeploymentImageResult.Found(
                new ImageCommitSha.Known("f5a1c2d")
        ));
        assertThat(dockerImageClient.operations)
                .containsExactly("pull:ghcr.io/deployko/api:1.0.0", "labels:ghcr.io/deployko/api:1.0.0");
    }

    @Test
    void returns_unknown_commit_sha_when_revision_label_is_missing() {
        ResolveDeploymentImagePort.ResolveDeploymentImageResult result =
                adapter.resolveDeploymentImage(IMAGE_REPOSITORY, IMAGE_VERSION);

        assertThat(result).isEqualTo(new ResolveDeploymentImagePort.ResolveDeploymentImageResult.Found(
                new ImageCommitSha.Unknown()
        ));
    }

    @Test
    void returns_image_not_found_when_pull_reports_missing_image() {
        dockerImageClient.pullException = new NotFoundException("manifest unknown");

        ResolveDeploymentImagePort.ResolveDeploymentImageResult result =
                adapter.resolveDeploymentImage(IMAGE_REPOSITORY, IMAGE_VERSION);

        assertThat(result).isInstanceOf(ResolveDeploymentImagePort.ResolveDeploymentImageResult.ImageNotFound.class);
        assertThat(dockerImageClient.operations).containsExactly("pull:ghcr.io/deployko/api:1.0.0");
    }

    @Test
    void returns_image_not_found_when_pull_callback_reports_missing_manifest() {
        dockerImageClient.pullRuntimeException = new DockerClientException("Could not pull image: manifest unknown");

        ResolveDeploymentImagePort.ResolveDeploymentImageResult result =
                adapter.resolveDeploymentImage(IMAGE_REPOSITORY, IMAGE_VERSION);

        assertThat(result).isInstanceOf(ResolveDeploymentImagePort.ResolveDeploymentImageResult.ImageNotFound.class);
    }

    @Test
    void returns_failure_when_docker_reports_unexpected_error() {
        dockerImageClient.pullException = new DockerException("docker unavailable", 500);

        ResolveDeploymentImagePort.ResolveDeploymentImageResult result =
                adapter.resolveDeploymentImage(IMAGE_REPOSITORY, IMAGE_VERSION);

        assertThat(result).isInstanceOf(ResolveDeploymentImagePort.ResolveDeploymentImageResult.Failure.class);
    }

    private static final class FakeDockerImageClient implements DockerImageClient {

        private final List<String> operations = new ArrayList<>();
        private final Map<String, String> labels = new LinkedHashMap<>();
        private DockerException pullException;
        private RuntimeException pullRuntimeException;

        @Override
        public void pullImage(String imageReference) {
            operations.add("pull:" + imageReference);
            if (pullException != null) {
                throw pullException;
            }
            if (pullRuntimeException != null) {
                throw pullRuntimeException;
            }
        }

        @Override
        public Map<String, String> imageLabels(String imageReference) {
            operations.add("labels:" + imageReference);
            return labels;
        }
    }
}
