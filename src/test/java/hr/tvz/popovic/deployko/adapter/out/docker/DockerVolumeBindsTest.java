package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMounts;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DockerVolumeBindsTest {

    @Test
    void maps_bind_mount_to_docker_bind() {
        VolumeMounts volumeMounts = VolumeMounts.empty()
                .add(new VolumeMount.BindMount(
                        new VolumeMount.HostPath("/opt/deployko/config"),
                        new VolumeMount.Target("/app/config"),
                        true
                ));

        List<Bind> binds = DockerVolumeBinds.from(volumeMounts);

        assertThat(binds).hasSize(1);
        Bind bind = binds.getFirst();
        assertThat(bind.getPath()).isEqualTo("/opt/deployko/config");
        assertThat(bind.getVolume().getPath()).isEqualTo("/app/config");
        assertThat(bind.getAccessMode()).isEqualTo(AccessMode.ro);
    }

    @Test
    void maps_named_volume_mount_to_docker_bind() {
        VolumeMounts volumeMounts = VolumeMounts.empty()
                .add(new VolumeMount.NamedVolumeMount(
                        new VolumeMount.VolumeName("deployko_data"),
                        new VolumeMount.Target("/var/lib/deployko"),
                        false
                ));

        List<Bind> binds = DockerVolumeBinds.from(volumeMounts);

        assertThat(binds).hasSize(1);
        Bind bind = binds.getFirst();
        assertThat(bind.getPath()).isEqualTo("deployko_data");
        assertThat(bind.getVolume().getPath()).isEqualTo("/var/lib/deployko");
        assertThat(bind.getAccessMode()).isEqualTo(AccessMode.rw);
    }

    @Test
    void maps_empty_volume_mounts_to_empty_list() {
        assertThat(DockerVolumeBinds.from(VolumeMounts.empty()))
                .isEmpty();
    }
}
