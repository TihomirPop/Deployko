package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMounts;
import java.util.List;
import java.util.Objects;

final class DockerVolumeBinds {

    private DockerVolumeBinds() {
    }

    static List<Bind> from(VolumeMounts volumeMounts) {
        Objects.requireNonNull(volumeMounts, "volumeMounts must not be null");

        return volumeMounts.asMap().values().stream()
                .map(DockerVolumeBinds::from)
                .toList();
    }

    private static Bind from(VolumeMount volumeMount) {
        AccessMode accessMode = volumeMount.readOnly() ? AccessMode.ro : AccessMode.rw;

        return switch (volumeMount) {
            case VolumeMount.BindMount bindMount -> new Bind(
                    bindMount.source().value(),
                    new Volume(bindMount.target().value()),
                    accessMode
            );
            case VolumeMount.NamedVolumeMount namedVolumeMount -> new Bind(
                    namedVolumeMount.source().value(),
                    new Volume(namedVolumeMount.target().value()),
                    accessMode
            );
        };
    }
}
