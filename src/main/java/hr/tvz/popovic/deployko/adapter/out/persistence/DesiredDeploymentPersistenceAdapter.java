package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
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

@Component
public final class DesiredDeploymentPersistenceAdapter
        implements UpsertDesiredDeploymentPort, UpdateDesiredDeploymentStatePort {

    private static final Logger log = LoggerFactory.getLogger(DesiredDeploymentPersistenceAdapter.class);

    private final DSLContext dsl;
    private final JooqTransactionHelper transactions;

    public DesiredDeploymentPersistenceAdapter(DSLContext dsl, JooqTransactionHelper transactions) {
        this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
        this.transactions = Objects.requireNonNull(transactions, "transactions must not be null");
    }

    @Override
    public UpsertDesiredDeploymentResult upsert(DesiredDeployment desiredDeployment) {
        Objects.requireNonNull(desiredDeployment, "desiredDeployment must not be null");

        try {
            return transactions.inTransaction(transactionalDsl -> {
                Optional<UUID> serviceId = ServiceIdRecords.find(transactionalDsl, desiredDeployment.serviceName());
                if (serviceId.isEmpty()) {
                    return new UpsertDesiredDeploymentResult.ServiceNotFound();
                }

                upsertDesiredDeployment(transactionalDsl, serviceId.get(), desiredDeployment);
                DesiredDeploymentRuntimeConfigurationRecords.replace(
                        transactionalDsl,
                        serviceId.get(),
                        desiredDeployment.runtimeConfiguration()
                );
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
            Optional<UUID> serviceId = ServiceIdRecords.find(dsl, serviceName);
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

    private static void upsertDesiredDeployment(DSLContext dsl, UUID serviceId, DesiredDeployment desiredDeployment) {
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
}
