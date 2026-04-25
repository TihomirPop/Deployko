package hr.tvz.popovic.deployko.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PortMappingsTest {

    @Test
    void creates_port_mappings_when_entries_are_empty() {
        assertThat(PortMappings.empty().entries()).isEmpty();
    }

    @Test
    void copies_entries_defensively() {
        Map<Port, Port> entries = new LinkedHashMap<>();
        entries.put(new Port(8080, Port.Protocol.TCP), new Port(80, Port.Protocol.TCP));

        PortMappings portMappings = new PortMappings(entries);
        entries.put(new Port(8443, Port.Protocol.TCP), new Port(443, Port.Protocol.TCP));

        assertThat(portMappings.entries())
                .containsExactly(Map.entry(new Port(8080, Port.Protocol.TCP), new Port(80, Port.Protocol.TCP)));
    }

    @Test
    void throws_when_entries_are_null() {
        assertThatThrownBy(() -> new PortMappings(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_entries_contain_null_host_port() {
        Map<Port, Port> entries = new LinkedHashMap<>();
        entries.put(null, new Port(80, Port.Protocol.TCP));

        assertThatThrownBy(() -> new PortMappings(entries))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_entries_contain_null_container_port() {
        Map<Port, Port> entries = new LinkedHashMap<>();
        entries.put(new Port(8080, Port.Protocol.TCP), null);

        assertThatThrownBy(() -> new PortMappings(entries))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_entries_reuse_container_port() {
        Map<Port, Port> entries = new LinkedHashMap<>();
        entries.put(new Port(8080, Port.Protocol.TCP), new Port(80, Port.Protocol.TCP));
        entries.put(new Port(8081, Port.Protocol.TCP), new Port(80, Port.Protocol.TCP));

        assertThatThrownBy(() -> new PortMappings(entries))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adds_port_mapping_when_ports_are_unique() {
        PortMappings portMappings = PortMappings.empty()
                .add(new Port(8080, Port.Protocol.TCP), new Port(80, Port.Protocol.TCP));

        assertThat(portMappings.entries())
                .containsExactly(Map.entry(new Port(8080, Port.Protocol.TCP), new Port(80, Port.Protocol.TCP)));
    }

    @Test
    void throws_when_adding_duplicate_host_port() {
        PortMappings portMappings = new PortMappings(Map.of(
                new Port(8080, Port.Protocol.TCP), new Port(80, Port.Protocol.TCP)
        ));

        assertThatThrownBy(() -> portMappings.add(
                new Port(8080, Port.Protocol.TCP),
                new Port(81, Port.Protocol.TCP)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_adding_duplicate_container_port() {
        PortMappings portMappings = new PortMappings(Map.of(
                new Port(8080, Port.Protocol.TCP), new Port(80, Port.Protocol.TCP)
        ));

        assertThatThrownBy(() -> portMappings.add(
                new Port(8081, Port.Protocol.TCP),
                new Port(80, Port.Protocol.TCP)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void replaces_port_mapping_for_existing_host_port() {
        Port hostPort = new Port(8080, Port.Protocol.TCP);
        PortMappings portMappings = new PortMappings(Map.of(
                hostPort, new Port(80, Port.Protocol.TCP)
        ));

        PortMappings replacedPortMappings = portMappings.replace(hostPort, new Port(81, Port.Protocol.TCP));

        assertThat(replacedPortMappings.getByHostPort(hostPort))
                .contains(new Port(81, Port.Protocol.TCP));
        assertThat(portMappings.getByHostPort(hostPort))
                .contains(new Port(80, Port.Protocol.TCP));
    }

    @Test
    void throws_when_replacing_missing_host_port() {
        assertThatThrownBy(() -> PortMappings.empty().replace(
                new Port(8080, Port.Protocol.TCP),
                new Port(80, Port.Protocol.TCP)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_replacing_with_existing_container_port() {
        PortMappings portMappings = new PortMappings(Map.of(
                new Port(8080, Port.Protocol.TCP), new Port(80, Port.Protocol.TCP),
                new Port(8443, Port.Protocol.TCP), new Port(443, Port.Protocol.TCP)
        ));

        assertThatThrownBy(() -> portMappings.replace(
                new Port(8080, Port.Protocol.TCP),
                new Port(443, Port.Protocol.TCP)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removes_port_mapping_by_host_port() {
        PortMappings portMappings = new PortMappings(Map.of(
                new Port(8080, Port.Protocol.TCP), new Port(80, Port.Protocol.TCP),
                new Port(8443, Port.Protocol.TCP), new Port(443, Port.Protocol.TCP)
        ));

        PortMappings remainingPortMappings = portMappings.removeByHostPort(new Port(8080, Port.Protocol.TCP));

        assertThat(remainingPortMappings.entries())
                .containsExactly(Map.entry(new Port(8443, Port.Protocol.TCP), new Port(443, Port.Protocol.TCP)));
    }

    @Test
    void returns_port_mapping_by_host_port() {
        Port hostPort = new Port(8080, Port.Protocol.TCP);
        Port containerPort = new Port(80, Port.Protocol.TCP);
        PortMappings portMappings = new PortMappings(Map.of(hostPort, containerPort));

        assertThat(portMappings.getByHostPort(hostPort))
                .contains(containerPort);
    }

    @Test
    void returns_entries_as_map() {
        Port hostPort = new Port(8080, Port.Protocol.TCP);
        Port containerPort = new Port(80, Port.Protocol.TCP);
        PortMappings portMappings = new PortMappings(Map.of(hostPort, containerPort));

        assertThat(portMappings.asMap())
                .containsExactly(Map.entry(hostPort, containerPort));
    }
}
