package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.enums.PortProtocol;
import hr.tvz.popovic.deployko.application.domain.model.Port;

final class PortProtocols {

    private PortProtocols() {
    }

    static PortProtocol toJooq(Port.Protocol protocol) {
        return switch (protocol) {
            case TCP -> PortProtocol.TCP;
            case UDP -> PortProtocol.UDP;
        };
    }

    static Port.Protocol toDomain(PortProtocol protocol) {
        return switch (protocol) {
            case TCP -> Port.Protocol.TCP;
            case UDP -> Port.Protocol.UDP;
        };
    }
}
