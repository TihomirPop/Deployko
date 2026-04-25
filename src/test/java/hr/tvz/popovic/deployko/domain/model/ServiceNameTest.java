package hr.tvz.popovic.deployko.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ServiceNameTest {

    @Test
    void creates_service_name_when_value_is_valid() {
        ServiceName serviceName = new ServiceName("deployko.api-1");

        assertEquals("deployko.api-1", serviceName.value());
    }

    @Test
    void trims_service_name_value() {
        ServiceName serviceName = new ServiceName("  deployko.api-1  ");

        assertEquals("deployko.api-1", serviceName.value());
    }

    @Test
    void throws_when_value_is_null() {
        assertThrows(NullPointerException.class, () -> new ServiceName(null));
    }

    @Test
    void throws_when_value_is_blank() {
        assertThrows(IllegalArgumentException.class, () -> new ServiceName("   "));
    }

    @Test
    void throws_when_value_contains_unsupported_characters() {
        assertThrows(IllegalArgumentException.class, () -> new ServiceName("Deployko Api"));
    }
}
