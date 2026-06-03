package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.PortMappings;
import java.util.List;
import java.util.Objects;

final class DockerPortBindings {

    private DockerPortBindings() {
    }

    static List<PortBinding> from(PortMappings portMappings) {
        Objects.requireNonNull(portMappings, "portMappings must not be null");

        return portMappings.asMap().entrySet().stream()
                .map(entry -> new PortBinding(
                        Ports.Binding.bindPort(entry.getKey().value()),
                        exposedPort(entry.getValue())
                ))
                .toList();
    }

    private static ExposedPort exposedPort(Port port) {
        return switch (port.protocol()) {
            case TCP -> ExposedPort.tcp(port.value());
            case UDP -> ExposedPort.udp(port.value());
        };
    }
}
