package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import org.jooq.Record;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_VOLUME_MOUNTS;

record VolumeMountRecord(String mountType, String source) {

    private static final String BIND_MOUNT_TYPE = "BIND";
    private static final String VOLUME_MOUNT_TYPE = "VOLUME";

    static VolumeMountRecord from(VolumeMount volumeMount) {
        return switch (volumeMount) {
            case VolumeMount.BindMount bindMount -> new VolumeMountRecord(BIND_MOUNT_TYPE, bindMount.source().value());
            case VolumeMount.NamedVolumeMount namedVolumeMount ->
                    new VolumeMountRecord(VOLUME_MOUNT_TYPE, namedVolumeMount.source().value());
        };
    }

    static VolumeMount fromServiceRuntimeConfiguration(Record record) {
        String targetPath = record.get(SERVICE_VOLUME_MOUNTS.TARGET_PATH);
        String mountType = record.get(SERVICE_VOLUME_MOUNTS.MOUNT_TYPE);
        String source = record.get(SERVICE_VOLUME_MOUNTS.SOURCE);
        boolean readOnly = record.get(SERVICE_VOLUME_MOUNTS.READ_ONLY);

        return switch (mountType) {
            case BIND_MOUNT_TYPE -> new VolumeMount.BindMount(
                    new VolumeMount.HostPath(source),
                    new VolumeMount.Target(targetPath),
                    readOnly
            );
            case VOLUME_MOUNT_TYPE -> new VolumeMount.NamedVolumeMount(
                    new VolumeMount.VolumeName(source),
                    new VolumeMount.Target(targetPath),
                    readOnly
            );
            default -> throw new IllegalStateException("unknown mount type: " + mountType);
        };
    }
}
