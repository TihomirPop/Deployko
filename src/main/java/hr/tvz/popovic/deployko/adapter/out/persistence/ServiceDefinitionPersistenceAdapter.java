package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceByNamePort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceDefinitionPort;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICES;

@Component
public final class ServiceDefinitionPersistenceAdapter
        implements CreateServicePort, DeleteServiceByNamePort, FindServiceDefinitionPort {

    private static final Logger log = LoggerFactory.getLogger(ServiceDefinitionPersistenceAdapter.class);

    private final DSLContext dsl;
    private final JooqTransactionHelper transactions;
    private final ServiceRuntimeConfigurationRecords runtimeConfigurationRecords;

    public ServiceDefinitionPersistenceAdapter(DSLContext dsl, JooqTransactionHelper transactions) {
        this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
        this.transactions = Objects.requireNonNull(transactions, "transactions must not be null");
        this.runtimeConfigurationRecords = new ServiceRuntimeConfigurationRecords(dsl);
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

                ServiceRuntimeConfigurationRecords.insert(
                        transactionalDsl,
                        serviceId.get(),
                        service.runtimeConfiguration()
                );
                return new CreateServicePortResult.Success();
            });
        } catch (DataAccessException exception) {
            log.error("error while inserting service", exception);
            return new CreateServicePortResult.Failure();
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
                            runtimeConfigurationRecords.find(record.get(SERVICES.ID))
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
}
