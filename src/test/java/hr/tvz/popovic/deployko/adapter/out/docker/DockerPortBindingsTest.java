package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.model.PortBinding;
import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.PortMappings;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DockerPortBindingsTest {

    @Test
    void maps_tcp_port_mappings_to_docker_port_bindings() {
        PortMappings portMappings = PortMappings.empty()
                .add(new Port(8080), new Port(80));

        List<PortBinding> portBindings = DockerPortBindings.from(portMappings);

        assertThat(portBindings).hasSize(1);
        PortBinding portBinding = portBindings.getFirst();
        assertThat(portBinding.getBinding().getHostPortSpec()).isEqualTo("8080");
        assertThat(portBinding.getExposedPort().getPort()).isEqualTo(80);
        assertThat(portBinding.getExposedPort().getProtocol().toString()).isEqualTo("tcp");
    }

    @Test
    void maps_udp_port_mappings_to_docker_port_bindings() {
        PortMappings portMappings = PortMappings.empty()
                .add(new Port(8443, Port.Protocol.UDP), new Port(443, Port.Protocol.UDP));

        List<PortBinding> portBindings = DockerPortBindings.from(portMappings);

        assertThat(portBindings).hasSize(1);
        PortBinding portBinding = portBindings.getFirst();
        assertThat(portBinding.getBinding().getHostPortSpec()).isEqualTo("8443");
        assertThat(portBinding.getExposedPort().getPort()).isEqualTo(443);
        assertThat(portBinding.getExposedPort().getProtocol().toString()).isEqualTo("udp");
    }

    @Test
    void maps_empty_port_mappings_to_empty_list() {
        assertThat(DockerPortBindings.from(PortMappings.empty()))
                .isEmpty();
    }
}
