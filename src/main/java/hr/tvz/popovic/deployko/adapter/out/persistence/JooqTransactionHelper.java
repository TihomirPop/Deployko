package hr.tvz.popovic.deployko.adapter.out.persistence;

import java.util.Objects;
import java.util.function.Function;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

@Component
public final class JooqTransactionHelper {

    private final DSLContext dsl;

    JooqTransactionHelper(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
    }

    <T> T inTransaction(Function<DSLContext, T> action) {
        Objects.requireNonNull(action, "action must not be null");

        return dsl.transactionResult(configuration -> action.apply(DSL.using(configuration)));
    }
}
