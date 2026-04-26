package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachment;
import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.RuntimeConfiguration;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceByNamePort;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_ENVIRONMENT_VARIABLES;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_NETWORK_ATTACHMENTS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_PORT_MAPPINGS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_VOLUME_MOUNTS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICES;

@Component
public final class ServicePersistenceAdapter implements CreateServicePort, DeleteServiceByNamePort {

    private static final String BIND_MOUNT_TYPE = "BIND";
    private static final String VOLUME_MOUNT_TYPE = "VOLUME";
    private static final Logger log = LoggerFactory.getLogger(ServicePersistenceAdapter.class);

    private final DSLContext dsl;
    private final JooqTransactionHelper transactions;

    public ServicePersistenceAdapter(DSLContext dsl, JooqTransactionHelper transactions) {
        this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
        this.transactions = Objects.requireNonNull(transactions, "transactions must not be null");
    }

    @Override
    public CreateServicePortResult create(Service service) {
        Objects.requireNonNull(service, "service must not be null");

        try {
            return transactions.inTransaction(transactionalDsl -> {
                Optional<UUID> serviceId = insertService(transactionalDsl, service);
                if (serviceId.isEmpty()) {
                    return new CreateServicePortResult.AlreadyExists();
                }

                insertRuntimeConfiguration(transactionalDsl, serviceId.get(), service.runtimeConfiguration());
                return new CreateServicePortResult.Success();
            });
        } catch (DataAccessException exception) {
            log.error("error while inserting service", exception);
            return new CreateServicePortResult.Failure();
        }
    }

    @Override
    public DeleteServiceByNameResult deleteByName(ServiceName serviceName) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");

        try {
            int deletedRows = dsl
                    .deleteFrom(SERVICES)
                    .where(SERVICES.NAME.eq(serviceName.value()))
                    .execute();

            return switch (deletedRows) {
                case 0 -> new DeleteServiceByNameResult.NotFound();
                case 1 -> new DeleteServiceByNameResult.Deleted();
                default -> new DeleteServiceByNameResult.Failure();
            };
        } catch (DataAccessException _) {
            return new DeleteServiceByNameResult.Failure();
        }
    }

    private static Optional<UUID> insertService(DSLContext dsl, Service service) {
        return dsl
                .insertInto(SERVICES)
                .set(SERVICES.NAME, service.name().value())
                .set(SERVICES.IMAGE_REPOSITORY, service.imageRepository().value())
                .onConflict(SERVICES.NAME)
                .doNothing()
                .returningResult(SERVICES.ID)
                .fetchOptional(SERVICES.ID);
    }

    private static void insertRuntimeConfiguration(
            DSLContext dsl,
            UUID serviceId,
            RuntimeConfiguration runtimeConfiguration
    ) {
        insertEnvironmentVariables(dsl, serviceId, runtimeConfiguration);
        insertPortMappings(dsl, serviceId, runtimeConfiguration);
        insertVolumeMounts(dsl, serviceId, runtimeConfiguration);
        insertNetworkAttachments(dsl, serviceId, runtimeConfiguration);
    }

    private static void insertEnvironmentVariables(
            DSLContext dsl,
            UUID serviceId,
            RuntimeConfiguration runtimeConfiguration
    ) {
        for (var entry : runtimeConfiguration.environmentVariables().asMap().entrySet()) {
            EnvironmentVariables.Key key = entry.getKey();
            EnvironmentVariables.Value value = entry.getValue();

            dsl
                    .insertInto(SERVICE_ENVIRONMENT_VARIABLES)
                    .set(SERVICE_ENVIRONMENT_VARIABLES.SERVICE_ID, serviceId)
                    .set(SERVICE_ENVIRONMENT_VARIABLES.KEY, key.value())
                    .set(SERVICE_ENVIRONMENT_VARIABLES.VALUE, value.value())
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
            VolumeMountValues values = VolumeMountValues.from(volumeMount);

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

    private record VolumeMountValues(String mountType, String source) {

        static VolumeMountValues from(VolumeMount volumeMount) {
            return switch (volumeMount) {
                case VolumeMount.BindMount bindMount ->
                        new VolumeMountValues(BIND_MOUNT_TYPE, bindMount.source().value());
                case VolumeMount.NamedVolumeMount namedVolumeMount ->
                        new VolumeMountValues(VOLUME_MOUNT_TYPE, namedVolumeMount.source().value());
            };
        }
    }
}
