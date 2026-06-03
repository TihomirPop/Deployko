package hr.tvz.popovic.deployko.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ImageVersionTest {

    @Test
    void creates_image_version_when_tag_is_valid() {
        ImageVersion imageVersion = new ImageVersion("1.2.3-alpine_1");

        assertThat(imageVersion.value()).isEqualTo("1.2.3-alpine_1");
    }

    @Test
    void trims_image_version() {
        ImageVersion imageVersion = new ImageVersion("  latest  ");

        assertThat(imageVersion.value()).isEqualTo("latest");
    }

    @Test
    void throws_when_image_version_is_null() {
        assertThatThrownBy(() -> new ImageVersion(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_image_version_is_blank() {
        assertThatThrownBy(() -> new ImageVersion("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_image_version_starts_with_invalid_character() {
        assertThatThrownBy(() -> new ImageVersion(".latest"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_image_version_contains_invalid_character() {
        assertThatThrownBy(() -> new ImageVersion("feature/build"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
