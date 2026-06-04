package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.CreateServiceEnvironmentVariablePort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceEnvironmentVariablesPort;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_ENVIRONMENT_VARIABLES;

@Component
public final class ServiceEnvironmentVariablePersistenceAdapter
        implements FindServiceEnvironmentVariablesPort, CreateServiceEnvironmentVariablePort {

    private static final Logger log = LoggerFactory.getLogger(ServiceEnvironmentVariablePersistenceAdapter.class);

    private final DSLContext dsl;
    private final JooqTransactionHelper transactions;
    private final ServiceRuntimeConfigurationRecords runtimeConfigurationRecords;

    public ServiceEnvironmentVariablePersistenceAdapter(DSLContext dsl, JooqTransactionHelper transactions) {
        this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
        this.transactions = Objects.requireNonNull(transactions, "transactions must not be null");
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

    @Override
    public CreateServiceEnvironmentVariableResult createEnvironmentVariable(
            ServiceName serviceName,
            EnvironmentVariables.Key key,
            EnvironmentVariables.Value value
    ) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");

        try {
            return transactions.inTransaction(transactionalDsl -> {
                Optional<UUID> serviceId = ServiceIdRecords.find(transactionalDsl, serviceName);
                if (serviceId.isEmpty()) {
                    return new CreateServiceEnvironmentVariableResult.ServiceNotFound();
                }

                if (environmentVariableExists(transactionalDsl, serviceId.get(), key)) {
                    return new CreateServiceEnvironmentVariableResult.AlreadyExists();
                }

                insertEnvironmentVariable(transactionalDsl, serviceId.get(), key, value);
                return new CreateServiceEnvironmentVariableResult.Created();
            });
        } catch (DataAccessException exception) {
            log.error("error while creating service environment variable", exception);
            return new CreateServiceEnvironmentVariableResult.Failure();
        }
    }

    private static boolean environmentVariableExists(
            DSLContext dsl,
            UUID serviceId,
            EnvironmentVariables.Key key
    ) {
        return dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_ENVIRONMENT_VARIABLES)
                        .where(SERVICE_ENVIRONMENT_VARIABLES.SERVICE_ID.eq(serviceId))
                        .and(SERVICE_ENVIRONMENT_VARIABLES.KEY.eq(key.value()))
        );
    }

    private static void insertEnvironmentVariable(
            DSLContext dsl,
            UUID serviceId,
            EnvironmentVariables.Key key,
            EnvironmentVariables.Value value
    ) {
        dsl
                .insertInto(SERVICE_ENVIRONMENT_VARIABLES)
                .set(SERVICE_ENVIRONMENT_VARIABLES.SERVICE_ID, serviceId)
                .set(SERVICE_ENVIRONMENT_VARIABLES.KEY, key.value())
                .set(SERVICE_ENVIRONMENT_VARIABLES.VALUE, value.value())
                .execute();
    }
}
