package hr.tvz.popovic.deployko.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DockerImageTest {

    @Test
    void creates_docker_image_when_reference_is_valid() {
        DockerImage dockerImage = new DockerImage("ghcr.io/deployko/api:1.0.0");

        assertThat(dockerImage.value()).isEqualTo("ghcr.io/deployko/api:1.0.0");
    }

    @Test
    void creates_docker_image_when_reference_uses_digest() {
        String digest = "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        DockerImage dockerImage = new DockerImage("registry.example.com/deployko/api@" + digest);

        assertThat(dockerImage.value()).isEqualTo("registry.example.com/deployko/api@" + digest);
    }

    @Test
    void trims_docker_image_reference() {
        DockerImage dockerImage = new DockerImage("  nginx:latest  ");

        assertThat(dockerImage.value()).isEqualTo("nginx:latest");
    }

    @Test
    void throws_when_reference_is_null() {
        assertThatThrownBy(() -> new DockerImage(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_reference_is_blank() {
        assertThatThrownBy(() -> new DockerImage("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_reference_contains_invalid_characters() {
        assertThatThrownBy(() -> new DockerImage("Deployko/API:latest"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
