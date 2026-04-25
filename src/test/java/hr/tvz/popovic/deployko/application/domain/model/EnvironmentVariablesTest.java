package hr.tvz.popovic.deployko.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EnvironmentVariablesTest {

    @Test
    void creates_environment_variables_when_entries_are_empty() {
        EnvironmentVariables environmentVariables = new EnvironmentVariables(List.of());

        assertThat(environmentVariables.entries()).isEmpty();
    }

    @Test
    void copies_entries_defensively() {
        List<EnvironmentVariable> entries = new ArrayList<>();
        entries.add(new EnvironmentVariable("SPRING_PROFILES_ACTIVE", "prod"));

        EnvironmentVariables environmentVariables = new EnvironmentVariables(entries);
        entries.add(new EnvironmentVariable("SERVER_PORT", "8080"));

        assertThat(environmentVariables.entries()).hasSize(1);
    }

    @Test
    void throws_when_entries_are_null() {
        assertThatThrownBy(() -> new EnvironmentVariables(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_entries_contain_null_value() {
        assertThatThrownBy(() -> new EnvironmentVariables(List.of(new EnvironmentVariable("APP_NAME", "deployko"), null)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_entries_contain_duplicate_keys() {
        assertThatThrownBy(() -> new EnvironmentVariables(List.of(
                new EnvironmentVariable("APP_NAME", "deployko"),
                new EnvironmentVariable("APP_NAME", "deployko-api")
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adds_environment_variable_when_key_is_unique() {
        EnvironmentVariables environmentVariables = new EnvironmentVariables(List.of())
                .add(new EnvironmentVariable("APP_NAME", "deployko"));

        assertThat(environmentVariables.entries())
                .extracting(EnvironmentVariable::key)
                .containsExactly("APP_NAME");
    }

    @Test
    void throws_when_adding_duplicate_environment_variable_key() {
        EnvironmentVariables environmentVariables = new EnvironmentVariables(List.of(
                new EnvironmentVariable("APP_NAME", "deployko")
        ));

        assertThatThrownBy(() -> environmentVariables.add(new EnvironmentVariable("APP_NAME", "deployko-api")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void replaces_environment_variable_with_same_key() {
        EnvironmentVariables environmentVariables = new EnvironmentVariables(List.of(
                new EnvironmentVariable("APP_NAME", "deployko")
        ));

        EnvironmentVariables replacedEnvironmentVariables = environmentVariables.replace(
                new EnvironmentVariable("APP_NAME", "deployko-api")
        );

        assertThat(replacedEnvironmentVariables.getByKey("APP_NAME"))
                .contains(new EnvironmentVariable("APP_NAME", "deployko-api"));
        assertThat(environmentVariables.getByKey("APP_NAME"))
                .contains(new EnvironmentVariable("APP_NAME", "deployko"));
    }

    @Test
    void throws_when_replacing_missing_environment_variable_key() {
        EnvironmentVariables environmentVariables = new EnvironmentVariables(List.of());

        assertThatThrownBy(() -> environmentVariables.replace(new EnvironmentVariable("APP_NAME", "deployko")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removes_environment_variable_by_key() {
        EnvironmentVariables environmentVariables = new EnvironmentVariables(List.of(
                new EnvironmentVariable("APP_NAME", "deployko"),
                new EnvironmentVariable("SERVER_PORT", "8080")
        ));

        EnvironmentVariables remainingEnvironmentVariables = environmentVariables.removeByKey("APP_NAME");

        assertThat(remainingEnvironmentVariables.entries())
                .extracting(EnvironmentVariable::key)
                .containsExactly("SERVER_PORT");
    }

    @Test
    void returns_environment_variable_by_key() {
        EnvironmentVariables environmentVariables = new EnvironmentVariables(List.of(
                new EnvironmentVariable("APP_NAME", "deployko")
        ));

        assertThat(environmentVariables.getByKey("APP_NAME"))
                .contains(new EnvironmentVariable("APP_NAME", "deployko"));
    }

    @Test
    void returns_entries_as_list() {
        EnvironmentVariable environmentVariable = new EnvironmentVariable("APP_NAME", "deployko");
        EnvironmentVariables environmentVariables = new EnvironmentVariables(List.of(environmentVariable));

        assertThat(environmentVariables.asList()).containsExactly(environmentVariable);
    }
}
