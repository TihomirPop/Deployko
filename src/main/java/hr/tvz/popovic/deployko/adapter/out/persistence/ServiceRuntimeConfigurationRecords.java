package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachment;
import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachments;
import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.PortMappings;
import hr.tvz.popovic.deployko.application.domain.model.RuntimeConfiguration;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMounts;

import java.util.Objects;
import java.util.UUID;

import org.jooq.DSLContext;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_ENVIRONMENT_VARIABLES;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_NETWORK_ATTACHMENTS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_PORT_MAPPINGS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_VOLUME_MOUNTS;

final class ServiceRuntimeConfigurationRecords {

    private final DSLContext dsl;

    ServiceRuntimeConfigurationRecords(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
    }

    RuntimeConfiguration find(UUID serviceId) {
        return new RuntimeConfiguration(
                findEnvironmentVariables(serviceId),
                findPortMappings(serviceId),
                findVolumeMounts(serviceId),
                findNetworkAttachments(serviceId)
        );
    }

    PortMappings findPortMappings(UUID serviceId) {
        PortMappings portMappings = PortMappings.empty();

        var records = dsl
                .select(
                        SERVICE_PORT_MAPPINGS.HOST_PORT,
                        SERVICE_PORT_MAPPINGS.HOST_PROTOCOL,
                        SERVICE_PORT_MAPPINGS.CONTAINER_PORT,
                        SERVICE_PORT_MAPPINGS.CONTAINER_PROTOCOL
                )
                .from(SERVICE_PORT_MAPPINGS)
                .where(SERVICE_PORT_MAPPINGS.SERVICE_ID.eq(serviceId))
                .fetch();

        for (var record : records) {
            portMappings = portMappings.add(
                    new Port(
                            record.get(SERVICE_PORT_MAPPINGS.HOST_PORT),
                            Port.Protocol.valueOf(record.get(SERVICE_PORT_MAPPINGS.HOST_PROTOCOL))
                    ),
                    new Port(
                            record.get(SERVICE_PORT_MAPPINGS.CONTAINER_PORT),
                            Port.Protocol.valueOf(record.get(SERVICE_PORT_MAPPINGS.CONTAINER_PROTOCOL))
                    )
            );
        }

        return portMappings;
    }

    static void insert(DSLContext dsl, UUID serviceId, RuntimeConfiguration runtimeConfiguration) {
        insertEnvironmentVariables(dsl, serviceId, runtimeConfiguration);
        insertPortMappings(dsl, serviceId, runtimeConfiguration);
        insertVolumeMounts(dsl, serviceId, runtimeConfiguration);
        insertNetworkAttachments(dsl, serviceId, runtimeConfiguration);
    }

    EnvironmentVariables findEnvironmentVariables(UUID serviceId) {
        EnvironmentVariables environmentVariables = EnvironmentVariables.empty();

        var records = dsl
                .select(SERVICE_ENVIRONMENT_VARIABLES.KEY, SERVICE_ENVIRONMENT_VARIABLES.VALUE)
                .from(SERVICE_ENVIRONMENT_VARIABLES)
                .where(SERVICE_ENVIRONMENT_VARIABLES.SERVICE_ID.eq(serviceId))
                .fetch();

        for (var record : records) {
            environmentVariables = environmentVariables.add(
                    new EnvironmentVariables.Key(record.get(SERVICE_ENVIRONMENT_VARIABLES.KEY)),
                    new EnvironmentVariables.Value(record.get(SERVICE_ENVIRONMENT_VARIABLES.VALUE))
            );
        }

        return environmentVariables;
    }

    VolumeMounts findVolumeMounts(UUID serviceId) {
        VolumeMounts volumeMounts = VolumeMounts.empty();

        var records = dsl
                .select(
                        SERVICE_VOLUME_MOUNTS.TARGET_PATH,
                        SERVICE_VOLUME_MOUNTS.MOUNT_TYPE,
                        SERVICE_VOLUME_MOUNTS.SOURCE,
                        SERVICE_VOLUME_MOUNTS.READ_ONLY
                )
                .from(SERVICE_VOLUME_MOUNTS)
                .where(SERVICE_VOLUME_MOUNTS.SERVICE_ID.eq(serviceId))
                .fetch();

        for (var record : records) {
            volumeMounts = volumeMounts.add(VolumeMountRecord.fromServiceRuntimeConfiguration(record));
        }

        return volumeMounts;
    }

    NetworkAttachments findNetworkAttachments(UUID serviceId) {
        NetworkAttachments networkAttachments = NetworkAttachments.empty();

        var records = dsl
                .select(SERVICE_NETWORK_ATTACHMENTS.NETWORK_NAME)
                .from(SERVICE_NETWORK_ATTACHMENTS)
                .where(SERVICE_NETWORK_ATTACHMENTS.SERVICE_ID.eq(serviceId))
                .fetch();

        for (var record : records) {
            networkAttachments = networkAttachments.add(new NetworkAttachment(
                    new NetworkAttachment.NetworkName(record.get(SERVICE_NETWORK_ATTACHMENTS.NETWORK_NAME))
            ));
        }

        return networkAttachments;
    }

    private static void insertEnvironmentVariables(
            DSLContext dsl,
            UUID serviceId,
            RuntimeConfiguration runtimeConfiguration
    ) {
        for (var entry : runtimeConfiguration.environmentVariables().asMap().entrySet()) {
            dsl
                    .insertInto(SERVICE_ENVIRONMENT_VARIABLES)
                    .set(SERVICE_ENVIRONMENT_VARIABLES.SERVICE_ID, serviceId)
                    .set(SERVICE_ENVIRONMENT_VARIABLES.KEY, entry.getKey().value())
                    .set(SERVICE_ENVIRONMENT_VARIABLES.VALUE, entry.getValue().value())
                    .execute();
        }
    }

    private static void insertPortMappings(DSLContext dsl, UUID serviceId, RuntimeConfiguration runtimeConfiguration) {
        for (var entry : runtimeConfiguration.portMappings().asMap().entrySet()) {
            Port hostPort = entry.getKey();
            Port containerPort = entry.getValue();

            dsl
                    .insertInto(SERVICE_PORT_MAPPINGS)
                    .set(SERVICE_PORT_MAPPINGS.SERVICE_ID, serviceId)
                    .set(SERVICE_PORT_MAPPINGS.HOST_PORT, hostPort.value())
                    .set(SERVICE_PORT_MAPPINGS.HOST_PROTOCOL, hostPort.protocol().name())
                    .set(SERVICE_PORT_MAPPINGS.CONTAINER_PORT, containerPort.value())
                    .set(SERVICE_PORT_MAPPINGS.CONTAINER_PROTOCOL, containerPort.protocol().name())
                    .execute();
        }
    }

    private static void insertVolumeMounts(DSLContext dsl, UUID serviceId, RuntimeConfiguration runtimeConfiguration) {
        for (VolumeMount volumeMount : runtimeConfiguration.volumeMounts().asMap().values()) {
            VolumeMountRecord values = VolumeMountRecord.from(volumeMount);

            dsl
                    .insertInto(SERVICE_VOLUME_MOUNTS)
                    .set(SERVICE_VOLUME_MOUNTS.SERVICE_ID, serviceId)
                    .set(SERVICE_VOLUME_MOUNTS.TARGET_PATH, volumeMount.target().value())
                    .set(SERVICE_VOLUME_MOUNTS.MOUNT_TYPE, values.mountType())
                    .set(SERVICE_VOLUME_MOUNTS.SOURCE, values.source())
                    .set(SERVICE_VOLUME_MOUNTS.READ_ONLY, volumeMount.readOnly())
                    .execute();
        }
    }

    private static void insertNetworkAttachments(
            DSLContext dsl,
            UUID serviceId,
            RuntimeConfiguration runtimeConfiguration
    ) {
        for (NetworkAttachment networkAttachment : runtimeConfiguration.networkAttachments().asMap().values()) {
            dsl
                    .insertInto(SERVICE_NETWORK_ATTACHMENTS)
                    .set(SERVICE_NETWORK_ATTACHMENTS.SERVICE_ID, serviceId)
                    .set(SERVICE_NETWORK_ATTACHMENTS.NETWORK_NAME, networkAttachment.networkName().value())
                    .execute();
        }
    }
}
