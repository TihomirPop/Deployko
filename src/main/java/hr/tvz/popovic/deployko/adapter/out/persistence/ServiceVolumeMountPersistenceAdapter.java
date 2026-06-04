package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import hr.tvz.popovic.deployko.application.port.out.CreateServiceVolumeMountPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceVolumeMountsPort;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_VOLUME_MOUNTS;

@Component
public final class ServiceVolumeMountPersistenceAdapter
        implements FindServiceVolumeMountsPort, CreateServiceVolumeMountPort {

    private static final Logger log = LoggerFactory.getLogger(ServiceVolumeMountPersistenceAdapter.class);

    private final DSLContext dsl;
    private final JooqTransactionHelper transactions;
    private final ServiceRuntimeConfigurationRecords runtimeConfigurationRecords;

    public ServiceVolumeMountPersistenceAdapter(DSLContext dsl, JooqTransactionHelper transactions) {
        this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
        this.transactions = Objects.requireNonNull(transactions, "transactions must not be null");
        this.runtimeConfigurationRecords = new ServiceRuntimeConfigurationRecords(dsl);
    }

    @Override
    public FindServiceVolumeMountsResult findVolumeMounts(ServiceName serviceName) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");

        try {
            Optional<UUID> serviceId = ServiceIdRecords.find(dsl, serviceName);
            if (serviceId.isEmpty()) {
                return new FindServiceVolumeMountsResult.ServiceNotFound();
            }

            return new FindServiceVolumeMountsResult.Found(
                    runtimeConfigurationRecords.findVolumeMounts(serviceId.get())
            );
        } catch (DataAccessException exception) {
            log.error("error while finding service volume mounts", exception);
            return new FindServiceVolumeMountsResult.Failure();
        }
    }

    @Override
    public CreateServiceVolumeMountResult createVolumeMount(ServiceName serviceName, VolumeMount volumeMount) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(volumeMount, "volumeMount must not be null");

        try {
            return transactions.inTransaction(transactionalDsl -> {
                Optional<UUID> serviceId = ServiceIdRecords.find(transactionalDsl, serviceName);
                if (serviceId.isEmpty()) {
                    return new CreateServiceVolumeMountResult.ServiceNotFound();
                }

                if (volumeMountExists(transactionalDsl, serviceId.get(), volumeMount.target())) {
                    return new CreateServiceVolumeMountResult.AlreadyExists();
                }

                insertVolumeMount(transactionalDsl, serviceId.get(), volumeMount);
                return new CreateServiceVolumeMountResult.Created();
            });
        } catch (DataAccessException exception) {
            log.error("error while creating service volume mount", exception);
            return new CreateServiceVolumeMountResult.Failure();
        }
    }

    private static boolean volumeMountExists(
            DSLContext dsl,
            UUID serviceId,
            VolumeMount.Target target
    ) {
        return dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_VOLUME_MOUNTS)
                        .where(SERVICE_VOLUME_MOUNTS.SERVICE_ID.eq(serviceId))
                        .and(SERVICE_VOLUME_MOUNTS.TARGET_PATH.eq(target.value()))
        );
    }

    private static void insertVolumeMount(DSLContext dsl, UUID serviceId, VolumeMount volumeMount) {
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
