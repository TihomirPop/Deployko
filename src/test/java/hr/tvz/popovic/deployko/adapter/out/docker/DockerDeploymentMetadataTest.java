package hr.tvz.popovic.deployko.adapter.out.docker;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.RuntimeConfiguration;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DockerDeploymentMetadataTest {

    private static final DeploymentId DEPLOYMENT_ID = new DeploymentId(
            UUID.fromString("018f4b5d-9c64-7000-9f2e-4d8fbf9f1b22")
    );

    @Test
    void builds_image_reference_from_repository_and_version() {
        assertThat(DockerDeploymentMetadata.imageReference(desiredDeployment()))
                .isEqualTo("ghcr.io/deployko/api:1.0.0");
    }

    @Test
    void builds_deterministic_container_name_from_service_name() {
        assertThat(DockerDeploymentMetadata.containerName(desiredDeployment()))
                .isEqualTo("deployko-deployko-api");
    }

    @Test
    void builds_deployko_management_labels() {
        assertThat(DockerDeploymentMetadata.labels(desiredDeployment(), DEPLOYMENT_ID))
                .containsEntry("deployko.managed", "true")
                .containsEntry("deployko.service.name", "deployko-api")
                .containsEntry("deployko.image.repository", "ghcr.io/deployko/api")
                .containsEntry("deployko.image.version", "1.0.0")
                .containsEntry("deployko.deployment.id", "018f4b5d-9c64-7000-9f2e-4d8fbf9f1b22");
    }

    private static DesiredDeployment desiredDeployment() {
        return new DesiredDeployment(
                new ServiceName("deployko-api"),
                new ImageRepository("ghcr.io/deployko/api"),
                new ImageVersion("1.0.0"),
                RuntimeConfiguration.empty(),
                DesiredDeploymentState.RUNNING
        );
    }
}
