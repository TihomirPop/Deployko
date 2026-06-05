package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.FindLastCiDeploymentPort;
import hr.tvz.popovic.deployko.application.port.out.RecordCiDeploymentPort;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_CI_DEPLOYMENTS;

@Component
public final class CiDeploymentPersistenceAdapter implements FindLastCiDeploymentPort, RecordCiDeploymentPort {

    private static final Logger log = LoggerFactory.getLogger(CiDeploymentPersistenceAdapter.class);

    private final DSLContext dsl;

    public CiDeploymentPersistenceAdapter(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
    }

    @Override
    public FindLastCiDeploymentResult findLastCiDeployment(ServiceName serviceName) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");

        try {
            Optional<UUID> serviceId = ServiceIdRecords.find(dsl, serviceName);
            if (serviceId.isEmpty()) {
                return new FindLastCiDeploymentResult.ServiceNotFound();
            }

            OffsetDateTime deployedAt = dsl
                    .select(SERVICE_CI_DEPLOYMENTS.LAST_DEPLOYED_AT)
                    .from(SERVICE_CI_DEPLOYMENTS)
                    .where(SERVICE_CI_DEPLOYMENTS.SERVICE_ID.eq(serviceId.get()))
                    .fetchOptional(SERVICE_CI_DEPLOYMENTS.LAST_DEPLOYED_AT)
                    .orElse(null);

            if (deployedAt == null) {
                return new FindLastCiDeploymentResult.NotDeployed();
            }

            return new FindLastCiDeploymentResult.Found(deployedAt);
        } catch (DataAccessException exception) {
            log.error("error while finding last CI deployment", exception);
            return new FindLastCiDeploymentResult.Failure();
        }
    }

    @Override
    public RecordCiDeploymentResult recordCiDeployment(ServiceName serviceName, OffsetDateTime deployedAt) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(deployedAt, "deployedAt must not be null");

        try {
            Optional<UUID> serviceId = ServiceIdRecords.find(dsl, serviceName);
            if (serviceId.isEmpty()) {
                return new RecordCiDeploymentResult.ServiceNotFound();
            }

            dsl
                    .insertInto(SERVICE_CI_DEPLOYMENTS)
                    .set(SERVICE_CI_DEPLOYMENTS.SERVICE_ID, serviceId.get())
                    .set(SERVICE_CI_DEPLOYMENTS.LAST_DEPLOYED_AT, deployedAt)
                    .onConflict(SERVICE_CI_DEPLOYMENTS.SERVICE_ID)
                    .doUpdate()
                    .set(SERVICE_CI_DEPLOYMENTS.LAST_DEPLOYED_AT, deployedAt)
                    .execute();

            return new RecordCiDeploymentResult.Recorded();
        } catch (DataAccessException exception) {
            log.error("error while recording CI deployment", exception);
            return new RecordCiDeploymentResult.Failure();
        }
    }
}
