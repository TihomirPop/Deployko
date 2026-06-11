package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentAttempt;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentStatus;
import hr.tvz.popovic.deployko.application.domain.model.ImageCommitSha;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.FindDeploymentHistoryPort;
import hr.tvz.popovic.deployko.application.port.out.FindLatestDeploymentPort;
import hr.tvz.popovic.deployko.application.port.out.RecordDeploymentHistoryPort;
import hr.tvz.popovic.deployko.application.port.out.UpdateDeploymentStatusPort;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Record5;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DEPLOYMENT_HISTORY;

@Component
public final class DeploymentHistoryPersistenceAdapter
        implements RecordDeploymentHistoryPort, UpdateDeploymentStatusPort, FindLatestDeploymentPort,
        FindDeploymentHistoryPort {

    private static final Logger log = LoggerFactory.getLogger(DeploymentHistoryPersistenceAdapter.class);

    private final DSLContext dsl;

    public DeploymentHistoryPersistenceAdapter(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
    }

    @Override
    public RecordDeploymentHistoryResult recordDeployment(
            ServiceName serviceName,
            ImageVersion imageVersion,
            ImageCommitSha commitSha
    ) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(imageVersion, "imageVersion must not be null");
        Objects.requireNonNull(commitSha, "commitSha must not be null");

        try {
            Optional<UUID> serviceId = ServiceIdRecords.find(dsl, serviceName);
            if (serviceId.isEmpty()) {
                return new RecordDeploymentHistoryResult.ServiceNotFound();
            }

            UUID deploymentId = dsl
                    .insertInto(SERVICE_DEPLOYMENT_HISTORY)
                    .set(SERVICE_DEPLOYMENT_HISTORY.SERVICE_ID, serviceId.get())
                    .set(SERVICE_DEPLOYMENT_HISTORY.IMAGE_VERSION, imageVersion.value())
                    .set(SERVICE_DEPLOYMENT_HISTORY.COMMIT_SHA, commitShaValue(commitSha))
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
            return serviceId.map(uuid -> dsl
                    .select(
                            SERVICE_DEPLOYMENT_HISTORY.ID,
                            SERVICE_DEPLOYMENT_HISTORY.IMAGE_VERSION,
                            SERVICE_DEPLOYMENT_HISTORY.COMMIT_SHA,
                            SERVICE_DEPLOYMENT_HISTORY.STATUS,
                            SERVICE_DEPLOYMENT_HISTORY.RECORDED_AT
                    )
                    .from(SERVICE_DEPLOYMENT_HISTORY)
                    .where(SERVICE_DEPLOYMENT_HISTORY.SERVICE_ID.eq(uuid))
                    .orderBy(
                            SERVICE_DEPLOYMENT_HISTORY.RECORDED_AT.desc(),
                            SERVICE_DEPLOYMENT_HISTORY.ID.desc()
                    )
                    .limit(1)
                    .fetchOptional()
                    .map(DeploymentHistoryPersistenceAdapter::deploymentAttempt)
                    .<FindLatestDeploymentResult>map(FindLatestDeploymentResult.Found::new)
                    .orElseGet(FindLatestDeploymentResult.NotDeployed::new)).orElseGet(FindLatestDeploymentResult.ServiceNotFound::new);

        } catch (DataAccessException | IllegalArgumentException exception) {
            log.error("error while finding latest deployment history", exception);
            return new FindLatestDeploymentResult.Failure();
        }
    }

    @Override
    public FindDeploymentHistoryResult findDeploymentHistory(ServiceName serviceName, Optional<OffsetDateTime> since) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(since, "since must not be null");

        try {
            Optional<UUID> serviceId = ServiceIdRecords.find(dsl, serviceName);
            if (serviceId.isEmpty()) {
                return new FindDeploymentHistoryResult.ServiceNotFound();
            }

            var condition = SERVICE_DEPLOYMENT_HISTORY.SERVICE_ID.eq(serviceId.get());
            if (since.isPresent()) {
                condition = condition.and(SERVICE_DEPLOYMENT_HISTORY.RECORDED_AT.ge(since.get()));
            }

            List<DeploymentAttempt> attempts = dsl
                    .select(
                            SERVICE_DEPLOYMENT_HISTORY.ID,
                            SERVICE_DEPLOYMENT_HISTORY.IMAGE_VERSION,
                            SERVICE_DEPLOYMENT_HISTORY.COMMIT_SHA,
                            SERVICE_DEPLOYMENT_HISTORY.STATUS,
                            SERVICE_DEPLOYMENT_HISTORY.RECORDED_AT
                    )
                    .from(SERVICE_DEPLOYMENT_HISTORY)
                    .where(condition)
                    .orderBy(
                            SERVICE_DEPLOYMENT_HISTORY.RECORDED_AT.asc(),
                            SERVICE_DEPLOYMENT_HISTORY.ID.asc()
                    )
                    .fetch(DeploymentHistoryPersistenceAdapter::deploymentAttempt);

            return new FindDeploymentHistoryResult.Found(attempts);
        } catch (DataAccessException | IllegalArgumentException exception) {
            log.error("error while finding deployment history", exception);
            return new FindDeploymentHistoryResult.Failure();
        }
    }

    private static DeploymentAttempt deploymentAttempt(
            Record5<UUID, String, String, String, OffsetDateTime> record
    ) {
        return new DeploymentAttempt(
                new DeploymentId(record.value1()),
                new ImageVersion(record.value2()),
                commitShaFrom(record.value3()),
                DeploymentStatus.valueOf(record.value4()),
                record.value5()
        );
    }

    private static String commitShaValue(ImageCommitSha commitSha) {
        return switch (commitSha) {
            case ImageCommitSha.Known known -> known.value();
            case ImageCommitSha.Unknown _ -> null;
        };
    }

    private static ImageCommitSha commitShaFrom(String value) {
        return value == null
                ? new ImageCommitSha.Unknown()
                : new ImageCommitSha.Known(value);
    }
}
