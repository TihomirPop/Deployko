package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.enums.VolumeMountType;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import org.jooq.Record;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_VOLUME_MOUNTS;

record VolumeMountRecord(VolumeMountType mountType, String source) {

    static VolumeMountRecord from(VolumeMount volumeMount) {
        return switch (volumeMount) {
            case VolumeMount.BindMount bindMount ->
                    new VolumeMountRecord(VolumeMountType.BIND, bindMount.source().value());
            case VolumeMount.NamedVolumeMount namedVolumeMount ->
                    new VolumeMountRecord(VolumeMountType.VOLUME, namedVolumeMount.source().value());
        };
    }

    static VolumeMount fromServiceRuntimeConfiguration(Record record) {
        String targetPath = record.get(SERVICE_VOLUME_MOUNTS.TARGET_PATH);
        VolumeMountType mountType = record.get(SERVICE_VOLUME_MOUNTS.MOUNT_TYPE);
        String source = record.get(SERVICE_VOLUME_MOUNTS.SOURCE);
        boolean readOnly = record.get(SERVICE_VOLUME_MOUNTS.READ_ONLY);

        return switch (mountType) {
            case BIND -> new VolumeMount.BindMount(
                    new VolumeMount.HostPath(source),
                    new VolumeMount.Target(targetPath),
                    readOnly
            );
            case VOLUME -> new VolumeMount.NamedVolumeMount(
                    new VolumeMount.VolumeName(source),
                    new VolumeMount.Target(targetPath),
                    readOnly
            );
        };
    }
}
