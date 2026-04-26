package hr.tvz.popovic.deployko;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
                + "org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration"
})
public class ContextTest {

    @Test
    void context_loads() {

    }
}
