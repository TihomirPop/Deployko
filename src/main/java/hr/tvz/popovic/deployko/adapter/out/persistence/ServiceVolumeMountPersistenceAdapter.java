package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.FindServiceVolumeMountsPort;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public final class ServiceVolumeMountPersistenceAdapter implements FindServiceVolumeMountsPort {

    private static final Logger log = LoggerFactory.getLogger(ServiceVolumeMountPersistenceAdapter.class);

    private final DSLContext dsl;
    private final ServiceRuntimeConfigurationRecords runtimeConfigurationRecords;

    public ServiceVolumeMountPersistenceAdapter(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
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
}
