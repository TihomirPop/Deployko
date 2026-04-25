package hr.tvz.popovic.deployko.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VolumeMountTest {

    @Test
    void creates_bind_mount_when_values_are_valid() {
        VolumeMount.BindMount volumeMount = new VolumeMount.BindMount(
                new VolumeMount.HostPath("/opt/deployko/config"),
                new VolumeMount.Target("/app/config"),
                true
        );

        assertThat(volumeMount.source()).isEqualTo(new VolumeMount.HostPath("/opt/deployko/config"));
        assertThat(volumeMount.target()).isEqualTo(new VolumeMount.Target("/app/config"));
        assertThat(volumeMount.readOnly()).isTrue();
    }

    @Test
    void creates_named_volume_mount_when_values_are_valid() {
        VolumeMount.NamedVolumeMount volumeMount = new VolumeMount.NamedVolumeMount(
                new VolumeMount.VolumeName("deployko_data"),
                new VolumeMount.Target("/var/lib/app"),
                false
        );

        assertThat(volumeMount.source()).isEqualTo(new VolumeMount.VolumeName("deployko_data"));
        assertThat(volumeMount.target()).isEqualTo(new VolumeMount.Target("/var/lib/app"));
        assertThat(volumeMount.readOnly()).isFalse();
    }

    @Test
    void trims_host_path_value() {
        VolumeMount.HostPath hostPath = new VolumeMount.HostPath("  /opt/deployko/config  ");

        assertThat(hostPath.value()).isEqualTo("/opt/deployko/config");
    }

    @Test
    void trims_target_value() {
        VolumeMount.Target target = new VolumeMount.Target("  /app/config  ");

        assertThat(target.value()).isEqualTo("/app/config");
    }

    @Test
    void trims_volume_name_value() {
        VolumeMount.VolumeName volumeName = new VolumeMount.VolumeName("  deployko_data  ");

        assertThat(volumeName.value()).isEqualTo("deployko_data");
    }

    @Test
    void throws_when_bind_mount_source_is_null() {
        assertThatThrownBy(() -> new VolumeMount.BindMount(null, new VolumeMount.Target("/app/config"), true))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_bind_mount_target_is_null() {
        assertThatThrownBy(() -> new VolumeMount.BindMount(new VolumeMount.HostPath("/opt/deployko/config"), null, true))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_named_volume_mount_source_is_null() {
        assertThatThrownBy(() -> new VolumeMount.NamedVolumeMount(null, new VolumeMount.Target("/app/config"), true))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_named_volume_mount_target_is_null() {
        assertThatThrownBy(() -> new VolumeMount.NamedVolumeMount(new VolumeMount.VolumeName("deployko_data"), null, true))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_host_path_is_null() {
        assertThatThrownBy(() -> new VolumeMount.HostPath(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_host_path_is_blank() {
        assertThatThrownBy(() -> new VolumeMount.HostPath("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_host_path_is_not_absolute() {
        assertThatThrownBy(() -> new VolumeMount.HostPath("opt/deployko/config"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_target_is_null() {
        assertThatThrownBy(() -> new VolumeMount.Target(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_target_is_blank() {
        assertThatThrownBy(() -> new VolumeMount.Target("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_target_is_not_absolute() {
        assertThatThrownBy(() -> new VolumeMount.Target("app/config"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_volume_name_is_null() {
        assertThatThrownBy(() -> new VolumeMount.VolumeName(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_volume_name_is_blank() {
        assertThatThrownBy(() -> new VolumeMount.VolumeName("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_volume_name_is_invalid() {
        assertThatThrownBy(() -> new VolumeMount.VolumeName("deployko/data"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
