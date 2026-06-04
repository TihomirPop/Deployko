package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.PortMappings;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.GetServicePortMappingsUseCase;
import hr.tvz.popovic.deployko.application.port.out.FindServicePortMappingsPort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceRuntimeConfigurationDomainServiceTest {

    private static final ServiceName SERVICE_NAME = new ServiceName("deployko-api");
    private static final PortMappings PORT_MAPPINGS = PortMappings.empty()
            .add(new Port(8080), new Port(80));

    @Test
    void get_port_mappings_returns_success_when_service_exists() {
        ServiceRuntimeConfigurationDomainService service = new ServiceRuntimeConfigurationDomainService(
                _ -> new FindServicePortMappingsPort.FindServicePortMappingsResult.Found(PORT_MAPPINGS)
        );

        GetServicePortMappingsUseCase.GetServicePortMappingsResult result = service.getServicePortMappings(
                new GetServicePortMappingsUseCase.GetServicePortMappingsCommand(SERVICE_NAME)
        );

        assertThat(result)
                .isInstanceOf(GetServicePortMappingsUseCase.GetServicePortMappingsResult.Success.class);
        GetServicePortMappingsUseCase.GetServicePortMappingsResult.Success success =
                (GetServicePortMappingsUseCase.GetServicePortMappingsResult.Success) result;
        assertThat(success.portMappings()).isEqualTo(PORT_MAPPINGS);
    }

    @Test
    void get_port_mappings_returns_not_found_when_service_does_not_exist() {
        ServiceRuntimeConfigurationDomainService service = new ServiceRuntimeConfigurationDomainService(
                _ -> new FindServicePortMappingsPort.FindServicePortMappingsResult.ServiceNotFound()
        );

        GetServicePortMappingsUseCase.GetServicePortMappingsResult result = service.getServicePortMappings(
                new GetServicePortMappingsUseCase.GetServicePortMappingsCommand(SERVICE_NAME)
        );

        assertThat(result)
                .isInstanceOf(GetServicePortMappingsUseCase.GetServicePortMappingsResult.NotFound.class);
    }

    @Test
    void get_port_mappings_returns_failure_when_persistence_fails() {
        ServiceRuntimeConfigurationDomainService service = new ServiceRuntimeConfigurationDomainService(
                _ -> new FindServicePortMappingsPort.FindServicePortMappingsResult.Failure()
        );

        GetServicePortMappingsUseCase.GetServicePortMappingsResult result = service.getServicePortMappings(
                new GetServicePortMappingsUseCase.GetServicePortMappingsCommand(SERVICE_NAME)
        );

        assertThat(result)
                .isInstanceOf(GetServicePortMappingsUseCase.GetServicePortMappingsResult.Failure.class);
    }
}
