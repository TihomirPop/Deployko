package hr.tvz.popovic.deployko.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EnvironmentVariablesTest {

    @Test
    void creates_environment_variables_when_entries_are_empty() {
        EnvironmentVariables environmentVariables = EnvironmentVariables.empty();

        assertThat(environmentVariables.entries()).isEmpty();
    }

    @Test
    void copies_entries_defensively() {
        Map<EnvironmentVariables.Key, EnvironmentVariables.Value> entries = new LinkedHashMap<>();
        entries.put(new EnvironmentVariables.Key("SPRING_PROFILES_ACTIVE"), new EnvironmentVariables.Value("prod"));

        EnvironmentVariables environmentVariables = new EnvironmentVariables(entries);
        entries.put(new EnvironmentVariables.Key("SERVER_PORT"), new EnvironmentVariables.Value("8080"));

        assertThat(environmentVariables.entries())
                .containsExactly(Map.entry(
                        new EnvironmentVariables.Key("SPRING_PROFILES_ACTIVE"),
                        new EnvironmentVariables.Value("prod")
                ));
    }

    @Test
    void throws_when_entries_are_null() {
        assertThatThrownBy(() -> new EnvironmentVariables(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_entries_contain_null_key() {
        Map<EnvironmentVariables.Key, EnvironmentVariables.Value> entries = new LinkedHashMap<>();
        entries.put(null, new EnvironmentVariables.Value("deployko"));

        assertThatThrownBy(() -> new EnvironmentVariables(entries))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_entries_contain_null_value() {
        Map<EnvironmentVariables.Key, EnvironmentVariables.Value> entries = new LinkedHashMap<>();
        entries.put(new EnvironmentVariables.Key("APP_NAME"), null);

        assertThatThrownBy(() -> new EnvironmentVariables(entries))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_key_is_blank() {
        assertThatThrownBy(() -> new EnvironmentVariables.Key("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_key_is_invalid() {
        assertThatThrownBy(() -> new EnvironmentVariables.Key("APP-NAME"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_value_is_null() {
        assertThatThrownBy(() -> new EnvironmentVariables.Value(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_value_is_null_on_add() {
        EnvironmentVariables environmentVariables = EnvironmentVariables.empty();

        assertThatThrownBy(() -> environmentVariables.add(new EnvironmentVariables.Key("APP_NAME"), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void adds_environment_variable_when_key_is_unique() {
        EnvironmentVariables environmentVariables = EnvironmentVariables.empty()
                .add(new EnvironmentVariables.Key("APP_NAME"), new EnvironmentVariables.Value("deployko"));

        assertThat(environmentVariables.entries())
                .containsExactly(Map.entry(
                        new EnvironmentVariables.Key("APP_NAME"),
                        new EnvironmentVariables.Value("deployko")
                ));
    }

    @Test
    void throws_when_adding_duplicate_environment_variable_key() {
        EnvironmentVariables environmentVariables = new EnvironmentVariables(Map.of(
                new EnvironmentVariables.Key("APP_NAME"), new EnvironmentVariables.Value("deployko")
        ));

        assertThatThrownBy(() -> environmentVariables.add(
                new EnvironmentVariables.Key("APP_NAME"),
                new EnvironmentVariables.Value("deployko-api")
        ))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void replaces_environment_variable_with_same_key() {
        EnvironmentVariables environmentVariables = new EnvironmentVariables(Map.of(
                new EnvironmentVariables.Key("APP_NAME"), new EnvironmentVariables.Value("deployko")
        ));

        EnvironmentVariables replacedEnvironmentVariables = environmentVariables.replace(
                new EnvironmentVariables.Key("APP_NAME"),
                new EnvironmentVariables.Value("deployko-api")
        );

        assertThat(replacedEnvironmentVariables.getByKey(new EnvironmentVariables.Key("APP_NAME")))
                .contains(new EnvironmentVariables.Value("deployko-api"));
        assertThat(environmentVariables.getByKey(new EnvironmentVariables.Key("APP_NAME")))
                .contains(new EnvironmentVariables.Value("deployko"));
    }

    @Test
    void throws_when_replacing_missing_environment_variable_key() {
        EnvironmentVariables environmentVariables = EnvironmentVariables.empty();

        assertThatThrownBy(() -> environmentVariables.replace(
                new EnvironmentVariables.Key("APP_NAME"),
                new EnvironmentVariables.Value("deployko")
        ))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removes_environment_variable_by_key() {
        EnvironmentVariables environmentVariables = new EnvironmentVariables(Map.of(
                new EnvironmentVariables.Key("APP_NAME"), new EnvironmentVariables.Value("deployko"),
                new EnvironmentVariables.Key("SERVER_PORT"), new EnvironmentVariables.Value("8080")
        ));

        EnvironmentVariables remainingEnvironmentVariables = environmentVariables.removeByKey(
                new EnvironmentVariables.Key("APP_NAME")
        );

        assertThat(remainingEnvironmentVariables.entries())
                .containsExactly(Map.entry(
                        new EnvironmentVariables.Key("SERVER_PORT"),
                        new EnvironmentVariables.Value("8080")
                ));
    }

    @Test
    void returns_environment_variable_by_key() {
        EnvironmentVariables environmentVariables = new EnvironmentVariables(Map.of(
                new EnvironmentVariables.Key("APP_NAME"), new EnvironmentVariables.Value("deployko")
        ));

        assertThat(environmentVariables.getByKey(new EnvironmentVariables.Key("APP_NAME")))
                .contains(new EnvironmentVariables.Value("deployko"));
    }

    @Test
    void returns_entries_as_map() {
        EnvironmentVariables environmentVariables = new EnvironmentVariables(Map.of(
                new EnvironmentVariables.Key("APP_NAME"), new EnvironmentVariables.Value("deployko")
        ));

        assertThat(environmentVariables.asMap())
                .containsExactly(Map.entry(
                        new EnvironmentVariables.Key("APP_NAME"),
                        new EnvironmentVariables.Value("deployko")
                ));
    }

    @Test
    void trims_keys_on_creation_and_lookup() {
        EnvironmentVariables environmentVariables = new EnvironmentVariables(Map.of(
                new EnvironmentVariables.Key("  APP_NAME  "), new EnvironmentVariables.Value("deployko")
        ));

        assertThat(environmentVariables.containsKey(new EnvironmentVariables.Key("APP_NAME"))).isTrue();
        assertThat(environmentVariables.getByKey(new EnvironmentVariables.Key("  APP_NAME  ")))
                .contains(new EnvironmentVariables.Value("deployko"));
    }

    @Test
    void creates_empty_environment_variables_via_static_factory() {
        assertThat(EnvironmentVariables.empty().entries()).isEmpty();
    }
}
