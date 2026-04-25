package hr.tvz.popovic.deployko.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ServiceNameTest {

    @Test
    void creates_service_name_when_value_is_valid() {
        ServiceName serviceName = new ServiceName("deployko.api-1");

        assertThat(serviceName.value()).isEqualTo("deployko.api-1");
    }

    @Test
    void trims_service_name_value() {
        ServiceName serviceName = new ServiceName("  deployko.api-1  ");

        assertThat(serviceName.value()).isEqualTo("deployko.api-1");
    }

    @Test
    void throws_when_value_is_null() {
        assertThatThrownBy(() -> new ServiceName(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_value_is_blank() {
        assertThatThrownBy(() -> new ServiceName("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_value_contains_unsupported_characters() {
        assertThatThrownBy(() -> new ServiceName("Deployko Api"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
