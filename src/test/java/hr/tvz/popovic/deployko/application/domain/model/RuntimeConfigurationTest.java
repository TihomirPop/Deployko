package hr.tvz.popovic.deployko.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RuntimeConfigurationTest {

    @Test
    void creates_runtime_configuration_when_values_are_valid() {
        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration(
                EnvironmentVariables.empty(),
                PortMappings.empty(),
                VolumeMounts.empty(),
                NetworkAttachments.empty()
        );

        assertThat(runtimeConfiguration.environmentVariables()).isEqualTo(EnvironmentVariables.empty());
        assertThat(runtimeConfiguration.portMappings()).isEqualTo(PortMappings.empty());
        assertThat(runtimeConfiguration.volumeMounts()).isEqualTo(VolumeMounts.empty());
        assertThat(runtimeConfiguration.networkAttachments()).isEqualTo(NetworkAttachments.empty());
    }

    @Test
    void creates_empty_runtime_configuration_via_static_factory() {
        RuntimeConfiguration runtimeConfiguration = RuntimeConfiguration.empty();

        assertThat(runtimeConfiguration.environmentVariables()).isEqualTo(EnvironmentVariables.empty());
        assertThat(runtimeConfiguration.portMappings()).isEqualTo(PortMappings.empty());
        assertThat(runtimeConfiguration.volumeMounts()).isEqualTo(VolumeMounts.empty());
        assertThat(runtimeConfiguration.networkAttachments()).isEqualTo(NetworkAttachments.empty());
    }

    @Test
    void throws_when_environment_variables_are_null() {
        assertThatThrownBy(() -> new RuntimeConfiguration(
                null,
                PortMappings.empty(),
                VolumeMounts.empty(),
                NetworkAttachments.empty()
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_port_mappings_are_null() {
        assertThatThrownBy(() -> new RuntimeConfiguration(
                EnvironmentVariables.empty(),
                null,
                VolumeMounts.empty(),
                NetworkAttachments.empty()
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_volume_mounts_are_null() {
        assertThatThrownBy(() -> new RuntimeConfiguration(
                EnvironmentVariables.empty(),
                PortMappings.empty(),
                null,
                NetworkAttachments.empty()
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_network_attachments_are_null() {
        assertThatThrownBy(() -> new RuntimeConfiguration(
                EnvironmentVariables.empty(),
                PortMappings.empty(),
                VolumeMounts.empty(),
                null
        )).isInstanceOf(NullPointerException.class);
    }
}
