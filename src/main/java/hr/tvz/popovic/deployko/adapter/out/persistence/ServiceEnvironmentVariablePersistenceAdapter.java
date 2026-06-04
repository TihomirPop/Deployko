package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.FindServiceEnvironmentVariablesPort;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public final class ServiceEnvironmentVariablePersistenceAdapter implements FindServiceEnvironmentVariablesPort {

    private static final Logger log = LoggerFactory.getLogger(ServiceEnvironmentVariablePersistenceAdapter.class);

    private final DSLContext dsl;
    private final ServiceRuntimeConfigurationRecords runtimeConfigurationRecords;

    public ServiceEnvironmentVariablePersistenceAdapter(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
        this.runtimeConfigurationRecords = new ServiceRuntimeConfigurationRecords(dsl);
    }

    @Override
    public FindServiceEnvironmentVariablesResult findEnvironmentVariables(ServiceName serviceName) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");

        try {
            Optional<UUID> serviceId = ServiceIdRecords.find(dsl, serviceName);
            if (serviceId.isEmpty()) {
                return new FindServiceEnvironmentVariablesResult.ServiceNotFound();
            }

            return new FindServiceEnvironmentVariablesResult.Found(
                    runtimeConfigurationRecords.findEnvironmentVariables(serviceId.get())
            );
        } catch (DataAccessException exception) {
            log.error("error while finding service environment variables", exception);
            return new FindServiceEnvironmentVariablesResult.Failure();
        }
    }
}
