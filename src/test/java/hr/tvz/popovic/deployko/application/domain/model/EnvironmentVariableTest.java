package hr.tvz.popovic.deployko.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EnvironmentVariableTest {

    @Test
    void creates_environment_variable_when_values_are_valid() {
        EnvironmentVariable environmentVariable = new EnvironmentVariable("SPRING_PROFILES_ACTIVE", "prod");

        assertThat(environmentVariable.key()).isEqualTo("SPRING_PROFILES_ACTIVE");
        assertThat(environmentVariable.value()).isEqualTo("prod");
    }

    @Test
    void trims_environment_variable_key() {
        EnvironmentVariable environmentVariable = new EnvironmentVariable("  APP_PORT  ", "8080");

        assertThat(environmentVariable.key()).isEqualTo("APP_PORT");
    }

    @Test
    void allows_empty_environment_variable_value() {
        EnvironmentVariable environmentVariable = new EnvironmentVariable("OPTIONAL_VALUE", "");

        assertThat(environmentVariable.value()).isEmpty();
    }

    @Test
    void throws_when_key_is_null() {
        assertThatThrownBy(() -> new EnvironmentVariable(null, "value"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_value_is_null() {
        assertThatThrownBy(() -> new EnvironmentVariable("KEY", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_key_is_blank() {
        assertThatThrownBy(() -> new EnvironmentVariable("   ", "value"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_key_is_invalid() {
        assertThatThrownBy(() -> new EnvironmentVariable("APP-NAME", "value"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
