package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceByNamePort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceDefinitionPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceNamesByImageRepositoryPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceSummaryCandidatesPort;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DESIRED_DEPLOYMENTS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICES;

@Component
public final class ServiceDefinitionPersistenceAdapter
        implements CreateServicePort, DeleteServiceByNamePort, FindServiceDefinitionPort,
        FindServiceNamesByImageRepositoryPort, FindServiceSummaryCandidatesPort {

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
    public FindServiceNamesByImageRepositoryResult findServiceNamesByImageRepository(ImageRepository imageRepository) {
        Objects.requireNonNull(imageRepository, "imageRepository must not be null");

        try {
            List<ServiceName> serviceNames = dsl
                    .select(SERVICES.NAME)
                    .from(SERVICES)
                    .where(SERVICES.IMAGE_REPOSITORY.eq(imageRepository.value()))
                    .orderBy(SERVICES.NAME)
                    .fetch(SERVICES.NAME)
                    .stream()
                    .map(ServiceName::new)
                    .toList();

            return new FindServiceNamesByImageRepositoryResult.Found(serviceNames);
        } catch (DataAccessException exception) {
            log.error("error while finding service names for image repository {}", imageRepository.value(), exception);
            return new FindServiceNamesByImageRepositoryResult.Failure();
        }
    }

    @Override
    public FindServiceSummaryCandidatesResult findServiceSummaryCandidates() {
        try {
            List<ServiceSummaryCandidate> services = dsl
                    .select(
                            SERVICES.NAME,
                            SERVICES.IMAGE_REPOSITORY,
                            SERVICE_DESIRED_DEPLOYMENTS.IMAGE_VERSION,
                            SERVICE_DESIRED_DEPLOYMENTS.DESIRED_STATE
                    )
                    .from(SERVICES)
                    .leftJoin(SERVICE_DESIRED_DEPLOYMENTS)
                    .on(SERVICE_DESIRED_DEPLOYMENTS.SERVICE_ID.eq(SERVICES.ID))
                    .orderBy(SERVICES.NAME)
                    .fetch(record -> new ServiceSummaryCandidate(
                            new ServiceName(record.get(SERVICES.NAME)),
                            new ImageRepository(record.get(SERVICES.IMAGE_REPOSITORY)),
                            Optional.ofNullable(record.get(SERVICE_DESIRED_DEPLOYMENTS.IMAGE_VERSION))
                                    .map(ImageVersion::new),
                            Optional.ofNullable(record.get(SERVICE_DESIRED_DEPLOYMENTS.DESIRED_STATE))
                                    .map(DesiredDeploymentStates::toDomain)
                    ));

            return new FindServiceSummaryCandidatesResult.Found(services);
        } catch (DataAccessException | IllegalArgumentException exception) {
            log.error("error while finding service summary candidates", exception);
            return new FindServiceSummaryCandidatesResult.Failure();
        }
    }

    @Override
    public DeleteServiceByNameResult deleteByName(ServiceName serviceName) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");

        try {
            Optional<UUID> serviceId = ServiceIdRecords.find(dsl, serviceName);
            if (serviceId.isEmpty()) {
                return new DeleteServiceByNameResult.NotFound();
            }

            boolean deploymentExists = dsl.fetchExists(
                    SERVICE_DESIRED_DEPLOYMENTS,
                    SERVICE_DESIRED_DEPLOYMENTS.SERVICE_ID.eq(serviceId.get())
            );
            if (deploymentExists) {
                return new DeleteServiceByNameResult.DeploymentExists();
            }

            int deletedRows = dsl
                    .deleteFrom(SERVICES)
                    .where(SERVICES.ID.eq(serviceId.get()))
                    .execute();

            return switch (deletedRows) {
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
