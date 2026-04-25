package hr.tvz.popovic.deployko.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PortTest {

    @Test
    void creates_port_with_tcp_protocol_by_default() {
        Port port = new Port(8080);

        assertThat(port.value()).isEqualTo(8080);
        assertThat(port.protocol()).isEqualTo(Port.Protocol.TCP);
    }

    @Test
    void creates_port_when_values_are_valid() {
        Port port = new Port(8080, Port.Protocol.TCP);

        assertThat(port.value()).isEqualTo(8080);
        assertThat(port.protocol()).isEqualTo(Port.Protocol.TCP);
    }

    @Test
    void throws_when_protocol_is_null() {
        assertThatThrownBy(() -> new Port(8080, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_port_is_below_range() {
        assertThatThrownBy(() -> new Port(0, Port.Protocol.TCP))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_port_is_above_range() {
        assertThatThrownBy(() -> new Port(65_536, Port.Protocol.TCP))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
