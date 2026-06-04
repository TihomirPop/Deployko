package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICES;

final class ServiceIdRecords {

    private ServiceIdRecords() {
    }

    static Optional<UUID> find(DSLContext dsl, ServiceName serviceName) {
        return dsl
                .select(SERVICES.ID)
                .from(SERVICES)
                .where(SERVICES.NAME.eq(serviceName.value()))
                .fetchOptional(SERVICES.ID);
    }
}
