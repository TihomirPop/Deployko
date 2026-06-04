package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.FindServicePortMappingsPort;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_PORT_MAPPINGS;

@Component
public final class ServicePortMappingPersistenceAdapter
        implements FindServicePortMappingsPort, CreateServicePortMappingPort, DeleteServicePortMappingPort {

    private static final Logger log = LoggerFactory.getLogger(ServicePortMappingPersistenceAdapter.class);

    private final DSLContext dsl;
    private final JooqTransactionHelper transactions;
    private final ServiceRuntimeConfigurationRecords runtimeConfigurationRecords;

    public ServicePortMappingPersistenceAdapter(DSLContext dsl, JooqTransactionHelper transactions) {
        this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
        this.transactions = Objects.requireNonNull(transactions, "transactions must not be null");
        this.runtimeConfigurationRecords = new ServiceRuntimeConfigurationRecords(dsl);
    }

    @Override
    public FindServicePortMappingsResult findPortMappings(ServiceName serviceName) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");

        try {
            Optional<UUID> serviceId = ServiceIdRecords.find(dsl, serviceName);
            if (serviceId.isEmpty()) {
                return new FindServicePortMappingsResult.ServiceNotFound();
            }

            return new FindServicePortMappingsResult.Found(
                    runtimeConfigurationRecords.findPortMappings(serviceId.get())
            );
        } catch (DataAccessException exception) {
            log.error("error while finding service port mappings", exception);
            return new FindServicePortMappingsResult.Failure();
        }
    }

    @Override
    public CreateServicePortMappingResult createPortMapping(
            ServiceName serviceName,
            Port hostPort,
            Port containerPort
    ) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(hostPort, "hostPort must not be null");
        Objects.requireNonNull(containerPort, "containerPort must not be null");

        try {
            return transactions.inTransaction(transactionalDsl -> {
                Optional<UUID> serviceId = ServiceIdRecords.find(transactionalDsl, serviceName);
                if (serviceId.isEmpty()) {
                    return new CreateServicePortMappingResult.ServiceNotFound();
                }

                if (portMappingExists(transactionalDsl, serviceId.get(), hostPort, containerPort)) {
                    return new CreateServicePortMappingResult.AlreadyExists();
                }

                insertPortMapping(transactionalDsl, serviceId.get(), hostPort, containerPort);
                return new CreateServicePortMappingResult.Created();
            });
        } catch (DataAccessException exception) {
            log.error("error while creating service port mapping", exception);
            return new CreateServicePortMappingResult.Failure();
        }
    }

    @Override
    public DeleteServicePortMappingResult deletePortMapping(ServiceName serviceName, Port hostPort) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(hostPort, "hostPort must not be null");

        try {
            Optional<UUID> serviceId = ServiceIdRecords.find(dsl, serviceName);
            if (serviceId.isEmpty()) {
                return new DeleteServicePortMappingResult.ServiceNotFound();
            }

            int deletedRows = dsl
                    .deleteFrom(SERVICE_PORT_MAPPINGS)
                    .where(SERVICE_PORT_MAPPINGS.SERVICE_ID.eq(serviceId.get()))
                    .and(SERVICE_PORT_MAPPINGS.HOST_PORT.eq(hostPort.value()))
                    .and(SERVICE_PORT_MAPPINGS.HOST_PROTOCOL.eq(hostPort.protocol().name()))
                    .execute();

            return switch (deletedRows) {
                case 0 -> new DeleteServicePortMappingResult.PortMappingNotFound();
                case 1 -> new DeleteServicePortMappingResult.Deleted();
                default -> new DeleteServicePortMappingResult.Failure();
            };
        } catch (DataAccessException exception) {
            log.error("error while deleting service port mapping", exception);
            return new DeleteServicePortMappingResult.Failure();
        }
    }

    private static boolean portMappingExists(DSLContext dsl, UUID serviceId, Port hostPort, Port containerPort) {
        return dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_PORT_MAPPINGS)
                        .where(SERVICE_PORT_MAPPINGS.SERVICE_ID.eq(serviceId))
                        .and(SERVICE_PORT_MAPPINGS.HOST_PORT.eq(hostPort.value()))
                        .and(SERVICE_PORT_MAPPINGS.HOST_PROTOCOL.eq(hostPort.protocol().name()))
        ) || dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_PORT_MAPPINGS)
                        .where(SERVICE_PORT_MAPPINGS.SERVICE_ID.eq(serviceId))
                        .and(SERVICE_PORT_MAPPINGS.CONTAINER_PORT.eq(containerPort.value()))
                        .and(SERVICE_PORT_MAPPINGS.CONTAINER_PROTOCOL.eq(containerPort.protocol().name()))
        );
    }

    private static void insertPortMapping(DSLContext dsl, UUID serviceId, Port hostPort, Port containerPort) {
        dsl
                .insertInto(SERVICE_PORT_MAPPINGS)
                .set(SERVICE_PORT_MAPPINGS.SERVICE_ID, serviceId)
                .set(SERVICE_PORT_MAPPINGS.HOST_PORT, hostPort.value())
                .set(SERVICE_PORT_MAPPINGS.HOST_PROTOCOL, hostPort.protocol().name())
                .set(SERVICE_PORT_MAPPINGS.CONTAINER_PORT, containerPort.value())
                .set(SERVICE_PORT_MAPPINGS.CONTAINER_PROTOCOL, containerPort.protocol().name())
                .execute();
    }
}
