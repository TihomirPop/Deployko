package hr.tvz.popovic.deployko.adapter.out.docker;

import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DockerEnvironmentVariablesTest {

    @Test
    void maps_environment_variables_to_docker_env_values() {
        EnvironmentVariables environmentVariables = EnvironmentVariables.empty()
                .add(new EnvironmentVariables.Key("APP_ENV"), new EnvironmentVariables.Value("prod"))
                .add(new EnvironmentVariables.Key("JAVA_OPTS"), new EnvironmentVariables.Value("-Xmx512m"));

        assertThat(DockerEnvironmentVariables.from(environmentVariables))
                .containsExactly("APP_ENV=prod", "JAVA_OPTS=-Xmx512m");
    }

    @Test
    void maps_empty_environment_variables_to_empty_list() {
        assertThat(DockerEnvironmentVariables.from(EnvironmentVariables.empty()))
                .isEmpty();
    }
}
