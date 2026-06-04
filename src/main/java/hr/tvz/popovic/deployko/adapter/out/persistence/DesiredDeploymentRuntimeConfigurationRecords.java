package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachment;
import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.RuntimeConfiguration;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import java.util.UUID;
import org.jooq.DSLContext;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DESIRED_DEPLOYMENT_NETWORK_ATTACHMENTS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DESIRED_DEPLOYMENT_PORT_MAPPINGS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DESIRED_DEPLOYMENT_VOLUME_MOUNTS;

final class DesiredDeploymentRuntimeConfigurationRecords {

    private DesiredDeploymentRuntimeConfigurationRecords() {
    }

    static void replace(DSLContext dsl, UUID serviceId, RuntimeConfiguration runtimeConfiguration) {
        delete(dsl, serviceId);
        insertEnvironmentVariables(dsl, serviceId, runtimeConfiguration);
        insertPortMappings(dsl, serviceId, runtimeConfiguration);
        insertVolumeMounts(dsl, serviceId, runtimeConfiguration);
        insertNetworkAttachments(dsl, serviceId, runtimeConfiguration);
    }

    private static void delete(DSLContext dsl, UUID serviceId) {
        dsl.deleteFrom(SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES)
                .where(SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES.SERVICE_ID.eq(serviceId))
                .execute();
        dsl.deleteFrom(SERVICE_DESIRED_DEPLOYMENT_PORT_MAPPINGS)
                .where(SERVICE_DESIRED_DEPLOYMENT_PORT_MAPPINGS.SERVICE_ID.eq(serviceId))
                .execute();
        dsl.deleteFrom(SERVICE_DESIRED_DEPLOYMENT_VOLUME_MOUNTS)
                .where(SERVICE_DESIRED_DEPLOYMENT_VOLUME_MOUNTS.SERVICE_ID.eq(serviceId))
                .execute();
        dsl.deleteFrom(SERVICE_DESIRED_DEPLOYMENT_NETWORK_ATTACHMENTS)
                .where(SERVICE_DESIRED_DEPLOYMENT_NETWORK_ATTACHMENTS.SERVICE_ID.eq(serviceId))
                .execute();
    }

    private static void insertEnvironmentVariables(
            DSLContext dsl,
            UUID serviceId,
            RuntimeConfiguration runtimeConfiguration
    ) {
        for (var entry : runtimeConfiguration.environmentVariables().asMap().entrySet()) {
            dsl
                    .insertInto(SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES)
                    .set(SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES.SERVICE_ID, serviceId)
                    .set(SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES.KEY, entry.getKey().value())
                    .set(SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES.VALUE, entry.getValue().value())
                    .execute();
        }
    }

    private static void insertPortMappings(
            DSLContext dsl,
            UUID serviceId,
            RuntimeConfiguration runtimeConfiguration
    ) {
        for (var entry : runtimeConfiguration.portMappings().asMap().entrySet()) {
            Port hostPort = entry.getKey();
            Port containerPort = entry.getValue();

            dsl
                    .insertInto(SERVICE_DESIRED_DEPLOYMENT_PORT_MAPPINGS)
                    .set(SERVICE_DESIRED_DEPLOYMENT_PORT_MAPPINGS.SERVICE_ID, serviceId)
                    .set(SERVICE_DESIRED_DEPLOYMENT_PORT_MAPPINGS.HOST_PORT, hostPort.value())
                    .set(SERVICE_DESIRED_DEPLOYMENT_PORT_MAPPINGS.HOST_PROTOCOL, hostPort.protocol().name())
                    .set(SERVICE_DESIRED_DEPLOYMENT_PORT_MAPPINGS.CONTAINER_PORT, containerPort.value())
                    .set(SERVICE_DESIRED_DEPLOYMENT_PORT_MAPPINGS.CONTAINER_PROTOCOL, containerPort.protocol().name())
                    .execute();
        }
    }

    private static void insertVolumeMounts(
            DSLContext dsl,
            UUID serviceId,
            RuntimeConfiguration runtimeConfiguration
    ) {
        for (VolumeMount volumeMount : runtimeConfiguration.volumeMounts().asMap().values()) {
            VolumeMountRecord values = VolumeMountRecord.from(volumeMount);

            dsl
                    .insertInto(SERVICE_DESIRED_DEPLOYMENT_VOLUME_MOUNTS)
                    .set(SERVICE_DESIRED_DEPLOYMENT_VOLUME_MOUNTS.SERVICE_ID, serviceId)
                    .set(SERVICE_DESIRED_DEPLOYMENT_VOLUME_MOUNTS.TARGET_PATH, volumeMount.target().value())
                    .set(SERVICE_DESIRED_DEPLOYMENT_VOLUME_MOUNTS.MOUNT_TYPE, values.mountType())
                    .set(SERVICE_DESIRED_DEPLOYMENT_VOLUME_MOUNTS.SOURCE, values.source())
                    .set(SERVICE_DESIRED_DEPLOYMENT_VOLUME_MOUNTS.READ_ONLY, volumeMount.readOnly())
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
                    .insertInto(SERVICE_DESIRED_DEPLOYMENT_NETWORK_ATTACHMENTS)
                    .set(SERVICE_DESIRED_DEPLOYMENT_NETWORK_ATTACHMENTS.SERVICE_ID, serviceId)
                    .set(
                            SERVICE_DESIRED_DEPLOYMENT_NETWORK_ATTACHMENTS.NETWORK_NAME,
                            networkAttachment.networkName().value()
                    )
                    .execute();
        }
    }
}
