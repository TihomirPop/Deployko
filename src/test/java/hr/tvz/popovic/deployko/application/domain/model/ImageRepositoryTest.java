package hr.tvz.popovic.deployko.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ImageRepositoryTest {

    @Test
    void creates_image_repository_when_reference_is_valid() {
        ImageRepository imageRepository = new ImageRepository("ghcr.io/deployko/api");

        assertThat(imageRepository.value()).isEqualTo("ghcr.io/deployko/api");
    }

    @Test
    void trims_image_repository_reference() {
        ImageRepository imageRepository = new ImageRepository("  deployko/api  ");

        assertThat(imageRepository.value()).isEqualTo("deployko/api");
    }

    @Test
    void throws_when_reference_is_null() {
        assertThatThrownBy(() -> new ImageRepository(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_reference_is_blank() {
        assertThatThrownBy(() -> new ImageRepository("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_reference_contains_tag() {
        assertThatThrownBy(() -> new ImageRepository("deployko/api:latest"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_reference_contains_invalid_characters() {
        assertThatThrownBy(() -> new ImageRepository("Deployko/API"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
