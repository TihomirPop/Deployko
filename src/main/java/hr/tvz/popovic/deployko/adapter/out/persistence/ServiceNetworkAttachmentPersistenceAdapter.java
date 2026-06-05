package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachment;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.CreateServiceNetworkAttachmentPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceNetworkAttachmentPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceNetworkAttachmentsPort;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_NETWORK_ATTACHMENTS;

@Component
public final class ServiceNetworkAttachmentPersistenceAdapter
        implements FindServiceNetworkAttachmentsPort, CreateServiceNetworkAttachmentPort,
        DeleteServiceNetworkAttachmentPort {

    private static final Logger log = LoggerFactory.getLogger(ServiceNetworkAttachmentPersistenceAdapter.class);

    private final DSLContext dsl;
    private final JooqTransactionHelper transactions;
    private final ServiceRuntimeConfigurationRecords runtimeConfigurationRecords;

    public ServiceNetworkAttachmentPersistenceAdapter(DSLContext dsl, JooqTransactionHelper transactions) {
        this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
        this.transactions = Objects.requireNonNull(transactions, "transactions must not be null");
        this.runtimeConfigurationRecords = new ServiceRuntimeConfigurationRecords(dsl);
    }

    @Override
    public FindServiceNetworkAttachmentsResult findNetworkAttachments(ServiceName serviceName) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");

        try {
            Optional<UUID> serviceId = ServiceIdRecords.find(dsl, serviceName);
            if (serviceId.isEmpty()) {
                return new FindServiceNetworkAttachmentsResult.ServiceNotFound();
            }

            return new FindServiceNetworkAttachmentsResult.Found(
                    runtimeConfigurationRecords.findNetworkAttachments(serviceId.get())
            );
        } catch (DataAccessException exception) {
            log.error("error while finding service network attachments", exception);
            return new FindServiceNetworkAttachmentsResult.Failure();
        }
    }

    @Override
    public CreateServiceNetworkAttachmentResult createNetworkAttachment(
            ServiceName serviceName,
            NetworkAttachment networkAttachment
    ) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(networkAttachment, "networkAttachment must not be null");

        try {
            return transactions.inTransaction(transactionalDsl -> {
                Optional<UUID> serviceId = ServiceIdRecords.find(transactionalDsl, serviceName);
                if (serviceId.isEmpty()) {
                    return new CreateServiceNetworkAttachmentResult.ServiceNotFound();
                }

                if (networkAttachmentExists(transactionalDsl, serviceId.get(), networkAttachment.networkName())) {
                    return new CreateServiceNetworkAttachmentResult.AlreadyExists();
                }

                insertNetworkAttachment(transactionalDsl, serviceId.get(), networkAttachment);
                return new CreateServiceNetworkAttachmentResult.Created();
            });
        } catch (DataAccessException exception) {
            log.error("error while creating service network attachment", exception);
            return new CreateServiceNetworkAttachmentResult.Failure();
        }
    }

    @Override
    public DeleteServiceNetworkAttachmentResult deleteNetworkAttachment(
            ServiceName serviceName,
            NetworkAttachment.NetworkName networkName
    ) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(networkName, "networkName must not be null");

        try {
            Optional<UUID> serviceId = ServiceIdRecords.find(dsl, serviceName);
            if (serviceId.isEmpty()) {
                return new DeleteServiceNetworkAttachmentResult.ServiceNotFound();
            }

            int deletedRows = dsl
                    .deleteFrom(SERVICE_NETWORK_ATTACHMENTS)
                    .where(SERVICE_NETWORK_ATTACHMENTS.SERVICE_ID.eq(serviceId.get()))
                    .and(SERVICE_NETWORK_ATTACHMENTS.NETWORK_NAME.eq(networkName.value()))
                    .execute();

            return switch (deletedRows) {
                case 0 -> new DeleteServiceNetworkAttachmentResult.NetworkAttachmentNotFound();
                case 1 -> new DeleteServiceNetworkAttachmentResult.Deleted();
                default -> new DeleteServiceNetworkAttachmentResult.Failure();
            };
        } catch (DataAccessException exception) {
            log.error("error while deleting service network attachment", exception);
            return new DeleteServiceNetworkAttachmentResult.Failure();
        }
    }

    private static boolean networkAttachmentExists(
            DSLContext dsl,
            UUID serviceId,
            NetworkAttachment.NetworkName networkName
    ) {
        return dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_NETWORK_ATTACHMENTS)
                        .where(SERVICE_NETWORK_ATTACHMENTS.SERVICE_ID.eq(serviceId))
                        .and(SERVICE_NETWORK_ATTACHMENTS.NETWORK_NAME.eq(networkName.value()))
        );
    }

    private static void insertNetworkAttachment(
            DSLContext dsl,
            UUID serviceId,
            NetworkAttachment networkAttachment
    ) {
        dsl
                .insertInto(SERVICE_NETWORK_ATTACHMENTS)
                .set(SERVICE_NETWORK_ATTACHMENTS.SERVICE_ID, serviceId)
                .set(SERVICE_NETWORK_ATTACHMENTS.NETWORK_NAME, networkAttachment.networkName().value())
                .execute();
    }
}
