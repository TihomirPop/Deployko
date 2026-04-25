package hr.tvz.popovic.deployko.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ServiceTest {

    @Test
    void creates_service_when_values_are_valid() {
        Service service = new Service(
                new ServiceName("deployko-api"),
                new ImageRepository("ghcr.io/deployko/api"),
                RuntimeConfiguration.empty()
        );

        assertThat(service.name()).isEqualTo(new ServiceName("deployko-api"));
        assertThat(service.imageRepository()).isEqualTo(new ImageRepository("ghcr.io/deployko/api"));
        assertThat(service.runtimeConfiguration()).isEqualTo(RuntimeConfiguration.empty());
    }

    @Test
    void throws_when_name_is_null() {
        assertThatThrownBy(() -> new Service(
                null,
                new ImageRepository("ghcr.io/deployko/api"),
                RuntimeConfiguration.empty()
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_image_repository_is_null() {
        assertThatThrownBy(() -> new Service(
                new ServiceName("deployko-api"),
                null,
                RuntimeConfiguration.empty()
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_runtime_configuration_is_null() {
        assertThatThrownBy(() -> new Service(
                new ServiceName("deployko-api"),
                new ImageRepository("ghcr.io/deployko/api"),
                null
        )).isInstanceOf(NullPointerException.class);
    }
}
