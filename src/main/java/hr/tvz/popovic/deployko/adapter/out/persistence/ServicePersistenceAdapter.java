package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachment;
import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachments;
import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.PortMappings;
import hr.tvz.popovic.deployko.application.domain.model.RuntimeConfiguration;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMounts;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceByNamePort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceDefinitionPort;
import hr.tvz.popovic.deployko.application.port.out.UpdateDesiredDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.UpsertDesiredDeploymentPort;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DESIRED_DEPLOYMENTS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DESIRED_DEPLOYMENT_NETWORK_ATTACHMENTS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DESIRED_DEPLOYMENT_PORT_MAPPINGS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DESIRED_DEPLOYMENT_VOLUME_MOUNTS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_ENVIRONMENT_VARIABLES;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_NETWORK_ATTACHMENTS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_PORT_MAPPINGS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_VOLUME_MOUNTS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICES;

@Component
public final class ServicePersistenceAdapter
        implements CreateServicePort, DeleteServiceByNamePort, FindServiceDefinitionPort, UpsertDesiredDeploymentPort,
        UpdateDesiredDeploymentStatePort {

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
    public UpsertDesiredDeploymentResult upsert(DesiredDeployment desiredDeployment) {
        Objects.requireNonNull(desiredDeployment, "desiredDeployment must not be null");

        try {
            return transactions.inTransaction(transactionalDsl -> {
                Optional<UUID> serviceId = findServiceId(transactionalDsl, desiredDeployment.serviceName());
                if (serviceId.isEmpty()) {
                    return new UpsertDesiredDeploymentResult.ServiceNotFound();
                }

                upsertDesiredDeployment(transactionalDsl, serviceId.get(), desiredDeployment);
                replaceDesiredRuntimeConfiguration(transactionalDsl, serviceId.get(), desiredDeployment.runtimeConfiguration());
                return new UpsertDesiredDeploymentResult.Success();
            });
        } catch (DataAccessException exception) {
            log.error("error while upserting desired deployment", exception);
            return new UpsertDesiredDeploymentResult.Failure();
        }
    }

    @Override
    public UpdateDesiredDeploymentStateResult updateState(
            ServiceName serviceName,
            DesiredDeploymentState desiredState
    ) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(desiredState, "desiredState must not be null");

        try {
            Optional<UUID> serviceId = findServiceId(dsl, serviceName);
            if (serviceId.isEmpty()) {
                return new UpdateDesiredDeploymentStateResult.ServiceNotFound();
            }

            int updatedRows = dsl
                    .update(SERVICE_DESIRED_DEPLOYMENTS)
                    .set(SERVICE_DESIRED_DEPLOYMENTS.DESIRED_STATE, desiredState.name())
                    .set(SERVICE_DESIRED_DEPLOYMENTS.UPDATED_AT, OffsetDateTime.now())
                    .where(SERVICE_DESIRED_DEPLOYMENTS.SERVICE_ID.eq(serviceId.get()))
                    .execute();

            return switch (updatedRows) {
                case 0 -> new UpdateDesiredDeploymentStateResult.NotDeployed();
                case 1 -> new UpdateDesiredDeploymentStateResult.Success();
                default -> new UpdateDesiredDeploymentStateResult.Failure();
            };
        } catch (DataAccessException exception) {
            log.error("error while updating desired deployment state", exception);
            return new UpdateDesiredDeploymentStateResult.Failure();
        }
    }

    @Override
    public FindServiceDefinitionResult findByName(ServiceName serviceName) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");

        try {
            return dsl
                    .select(SERVICES.ID, SERVICES.IMAGE_REPOSITORY)
                    .from(SERVICES)
                    .where(SERVICES.NAME.eq(serviceName.value()))
                    .fetchOptional(record -> new Service(
                            serviceName,
                            new ImageRepository(record.get(SERVICES.IMAGE_REPOSITORY)),
                            findRuntimeConfiguration(record.get(SERVICES.ID))
                    ))
                    .<FindServiceDefinitionResult>map(FindServiceDefinitionResult.Found::new)
                    .orElseGet(FindServiceDefinitionResult.NotFound::new);
        } catch (DataAccessException exception) {
            log.error("error while finding service definition", exception);
            return new FindServiceDefinitionResult.Failure();
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

    private static Optional<UUID> findServiceId(DSLContext dsl, ServiceName serviceName) {
        return dsl
                .select(SERVICES.ID)
                .from(SERVICES)
                .where(SERVICES.NAME.eq(serviceName.value()))
                .fetchOptional(SERVICES.ID);
    }

    private RuntimeConfiguration findRuntimeConfiguration(UUID serviceId) {
        return new RuntimeConfiguration(
                findEnvironmentVariables(serviceId),
                findPortMappings(serviceId),
                findVolumeMounts(serviceId),
                findNetworkAttachments(serviceId)
        );
    }

    private EnvironmentVariables findEnvironmentVariables(UUID serviceId) {
        EnvironmentVariables environmentVariables = EnvironmentVariables.empty();

        var records = dsl
                .select(
                        SERVICE_ENVIRONMENT_VARIABLES.KEY,
                        SERVICE_ENVIRONMENT_VARIABLES.VALUE
                )
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

    private PortMappings findPortMappings(UUID serviceId) {
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

    private VolumeMounts findVolumeMounts(UUID serviceId) {
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
            volumeMounts = volumeMounts.add(toVolumeMount(record));
        }

        return volumeMounts;
    }

    private NetworkAttachments findNetworkAttachments(UUID serviceId) {
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

    private static VolumeMount toVolumeMount(org.jooq.Record record) {
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

    private static void upsertDesiredDeployment(
            DSLContext dsl,
            UUID serviceId,
            DesiredDeployment desiredDeployment
    ) {
        dsl
                .insertInto(SERVICE_DESIRED_DEPLOYMENTS)
                .set(SERVICE_DESIRED_DEPLOYMENTS.SERVICE_ID, serviceId)
                .set(SERVICE_DESIRED_DEPLOYMENTS.IMAGE_VERSION, desiredDeployment.imageVersion().value())
                .set(SERVICE_DESIRED_DEPLOYMENTS.DESIRED_STATE, desiredDeployment.desiredState().name())
                .onConflict(SERVICE_DESIRED_DEPLOYMENTS.SERVICE_ID)
                .doUpdate()
                .set(SERVICE_DESIRED_DEPLOYMENTS.IMAGE_VERSION, desiredDeployment.imageVersion().value())
                .set(SERVICE_DESIRED_DEPLOYMENTS.DESIRED_STATE, desiredDeployment.desiredState().name())
                .set(SERVICE_DESIRED_DEPLOYMENTS.UPDATED_AT, OffsetDateTime.now())
                .execute();
    }

    private static void replaceDesiredRuntimeConfiguration(
            DSLContext dsl,
            UUID serviceId,
            RuntimeConfiguration runtimeConfiguration
    ) {
        deleteDesiredRuntimeConfiguration(dsl, serviceId);
        insertDesiredEnvironmentVariables(dsl, serviceId, runtimeConfiguration);
        insertDesiredPortMappings(dsl, serviceId, runtimeConfiguration);
        insertDesiredVolumeMounts(dsl, serviceId, runtimeConfiguration);
        insertDesiredNetworkAttachments(dsl, serviceId, runtimeConfiguration);
    }

    private static void deleteDesiredRuntimeConfiguration(DSLContext dsl, UUID serviceId) {
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

    private static void insertDesiredEnvironmentVariables(
            DSLContext dsl,
            UUID serviceId,
            RuntimeConfiguration runtimeConfiguration
    ) {
        for (var entry : runtimeConfiguration.environmentVariables().asMap().entrySet()) {
            EnvironmentVariables.Key key = entry.getKey();
            EnvironmentVariables.Value value = entry.getValue();

            dsl
                    .insertInto(SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES)
                    .set(SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES.SERVICE_ID, serviceId)
                    .set(SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES.KEY, key.value())
                    .set(SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES.VALUE, value.value())
                    .execute();
        }
    }

    private static void insertDesiredPortMappings(
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

    private static void insertDesiredVolumeMounts(
            DSLContext dsl,
            UUID serviceId,
            RuntimeConfiguration runtimeConfiguration
    ) {
        for (VolumeMount volumeMount : runtimeConfiguration.volumeMounts().asMap().values()) {
            VolumeMountValues values = VolumeMountValues.from(volumeMount);

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

    private static void insertDesiredNetworkAttachments(
            DSLContext dsl,
            UUID serviceId,
            RuntimeConfiguration runtimeConfiguration
    ) {
        for (NetworkAttachment networkAttachment : runtimeConfiguration.networkAttachments().asMap().values()) {
            dsl
                    .insertInto(SERVICE_DESIRED_DEPLOYMENT_NETWORK_ATTACHMENTS)
                    .set(SERVICE_DESIRED_DEPLOYMENT_NETWORK_ATTACHMENTS.SERVICE_ID, serviceId)
                    .set(SERVICE_DESIRED_DEPLOYMENT_NETWORK_ATTACHMENTS.NETWORK_NAME, networkAttachment.networkName().value())
                    .execute();
        }
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
