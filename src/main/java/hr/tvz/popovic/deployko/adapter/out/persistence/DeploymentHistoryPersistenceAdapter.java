package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentAttempt;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentStatus;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.FindLatestDeploymentPort;
import hr.tvz.popovic.deployko.application.port.out.RecordDeploymentHistoryPort;
import hr.tvz.popovic.deployko.application.port.out.UpdateDeploymentStatusPort;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Record4;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DEPLOYMENT_HISTORY;

@Component
public final class DeploymentHistoryPersistenceAdapter
        implements RecordDeploymentHistoryPort, UpdateDeploymentStatusPort, FindLatestDeploymentPort {

    private static final Logger log = LoggerFactory.getLogger(DeploymentHistoryPersistenceAdapter.class);

    private final DSLContext dsl;

    public DeploymentHistoryPersistenceAdapter(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
    }

    @Override
    public RecordDeploymentHistoryResult recordDeployment(ServiceName serviceName, ImageVersion imageVersion) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(imageVersion, "imageVersion must not be null");

        try {
            Optional<UUID> serviceId = ServiceIdRecords.find(dsl, serviceName);
            if (serviceId.isEmpty()) {
                return new RecordDeploymentHistoryResult.ServiceNotFound();
            }

            UUID deploymentId = dsl
                    .insertInto(SERVICE_DEPLOYMENT_HISTORY)
                    .set(SERVICE_DEPLOYMENT_HISTORY.SERVICE_ID, serviceId.get())
                    .set(SERVICE_DEPLOYMENT_HISTORY.IMAGE_VERSION, imageVersion.value())
                    .set(SERVICE_DEPLOYMENT_HISTORY.STATUS, DeploymentStatus.IN_PROGRESS.name())
                    .returningResult(SERVICE_DEPLOYMENT_HISTORY.ID)
                    .fetchSingle(SERVICE_DEPLOYMENT_HISTORY.ID);

            return new RecordDeploymentHistoryResult.Recorded(new DeploymentId(deploymentId));
        } catch (DataAccessException exception) {
            log.error("error while recording deployment history", exception);
            return new RecordDeploymentHistoryResult.Failure();
        }
    }

    @Override
    public UpdateDeploymentStatusResult updateStatus(DeploymentId deploymentId, DeploymentStatus status) {
        Objects.requireNonNull(deploymentId, "deploymentId must not be null");
        Objects.requireNonNull(status, "status must not be null");

        try {
            int updated = dsl
                    .update(SERVICE_DEPLOYMENT_HISTORY)
                    .set(SERVICE_DEPLOYMENT_HISTORY.STATUS, status.name())
                    .where(SERVICE_DEPLOYMENT_HISTORY.ID.eq(deploymentId.value()))
                    .execute();

            return updated == 1
                    ? new UpdateDeploymentStatusResult.Success()
                    : new UpdateDeploymentStatusResult.DeploymentNotFound();
        } catch (DataAccessException exception) {
            log.error("error while updating deployment status", exception);
            return new UpdateDeploymentStatusResult.Failure();
        }
    }

    @Override
    public FindLatestDeploymentResult findLatestDeployment(ServiceName serviceName) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");

        try {
            Optional<UUID> serviceId = ServiceIdRecords.find(dsl, serviceName);
            if (serviceId.isEmpty()) {
                return new FindLatestDeploymentResult.ServiceNotFound();
            }

            return dsl
                    .select(
                            SERVICE_DEPLOYMENT_HISTORY.ID,
                            SERVICE_DEPLOYMENT_HISTORY.IMAGE_VERSION,
                            SERVICE_DEPLOYMENT_HISTORY.STATUS,
                            SERVICE_DEPLOYMENT_HISTORY.RECORDED_AT
                    )
                    .from(SERVICE_DEPLOYMENT_HISTORY)
                    .where(SERVICE_DEPLOYMENT_HISTORY.SERVICE_ID.eq(serviceId.get()))
                    .orderBy(
                            SERVICE_DEPLOYMENT_HISTORY.RECORDED_AT.desc(),
                            SERVICE_DEPLOYMENT_HISTORY.ID.desc()
                    )
                    .limit(1)
                    .fetchOptional()
                    .<FindLatestDeploymentResult>map(DeploymentHistoryPersistenceAdapter::latestDeploymentFound)
                    .orElseGet(FindLatestDeploymentResult.NotDeployed::new);
        } catch (DataAccessException | IllegalArgumentException exception) {
            log.error("error while finding latest deployment history", exception);
            return new FindLatestDeploymentResult.Failure();
        }
    }

    private static FindLatestDeploymentResult.Found latestDeploymentFound(
            Record4<UUID, String, String, OffsetDateTime> record
    ) {
        return new FindLatestDeploymentResult.Found(new DeploymentAttempt(
                new DeploymentId(record.value1()),
                new ImageVersion(record.value2()),
                DeploymentStatus.valueOf(record.value3()),
                record.value4()
        ));
    }
}
