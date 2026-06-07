package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.RecordDeploymentHistoryPort;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DEPLOYMENT_HISTORY;

@Component
public final class DeploymentHistoryPersistenceAdapter implements RecordDeploymentHistoryPort {

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

            dsl
                    .insertInto(SERVICE_DEPLOYMENT_HISTORY)
                    .set(SERVICE_DEPLOYMENT_HISTORY.SERVICE_ID, serviceId.get())
                    .set(SERVICE_DEPLOYMENT_HISTORY.IMAGE_VERSION, imageVersion.value())
                    .execute();

            return new RecordDeploymentHistoryResult.Recorded();
        } catch (DataAccessException exception) {
            log.error("error while recording deployment history", exception);
            return new RecordDeploymentHistoryResult.Failure();
        }
    }
}
