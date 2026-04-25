package hr.tvz.popovic.deployko.application.domain.model;

import java.util.Objects;

public record Port(int value, Protocol protocol) {

    public Port {
        Objects.requireNonNull(protocol, "protocol must not be null");

        if (value < 1 || value > 65_535) {
            throw new IllegalArgumentException("value must be between 1 and 65535");
        }
    }

    public enum Protocol {
        TCP,
        UDP
    }
}
