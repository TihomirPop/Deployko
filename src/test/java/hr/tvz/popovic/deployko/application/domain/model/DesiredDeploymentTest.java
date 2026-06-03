package hr.tvz.popovic.deployko.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DesiredDeploymentTest {

    @Test
    void creates_desired_deployment_when_values_are_valid() {
        DesiredDeployment desiredDeployment = desiredDeployment();

        assertThat(desiredDeployment.serviceName()).isEqualTo(new ServiceName("deployko-api"));
        assertThat(desiredDeployment.imageRepository()).isEqualTo(new ImageRepository("ghcr.io/deployko/api"));
        assertThat(desiredDeployment.imageVersion()).isEqualTo(new ImageVersion("1.0.0"));
        assertThat(desiredDeployment.runtimeConfiguration()).isEqualTo(RuntimeConfiguration.empty());
        assertThat(desiredDeployment.desiredState()).isEqualTo(DesiredDeploymentState.RUNNING);
    }

    @Test
    void throws_when_service_name_is_null() {
        assertThatThrownBy(() -> new DesiredDeployment(
                null,
                new ImageRepository("ghcr.io/deployko/api"),
                new ImageVersion("1.0.0"),
                RuntimeConfiguration.empty(),
                DesiredDeploymentState.RUNNING
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_image_repository_is_null() {
        assertThatThrownBy(() -> new DesiredDeployment(
                new ServiceName("deployko-api"),
                null,
                new ImageVersion("1.0.0"),
                RuntimeConfiguration.empty(),
                DesiredDeploymentState.RUNNING
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_image_version_is_null() {
        assertThatThrownBy(() -> new DesiredDeployment(
                new ServiceName("deployko-api"),
                new ImageRepository("ghcr.io/deployko/api"),
                null,
                RuntimeConfiguration.empty(),
                DesiredDeploymentState.RUNNING
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_runtime_configuration_is_null() {
        assertThatThrownBy(() -> new DesiredDeployment(
                new ServiceName("deployko-api"),
                new ImageRepository("ghcr.io/deployko/api"),
                new ImageVersion("1.0.0"),
                null,
                DesiredDeploymentState.RUNNING
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_desired_state_is_null() {
        assertThatThrownBy(() -> new DesiredDeployment(
                new ServiceName("deployko-api"),
                new ImageRepository("ghcr.io/deployko/api"),
                new ImageVersion("1.0.0"),
                RuntimeConfiguration.empty(),
                null
        )).isInstanceOf(NullPointerException.class);
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
