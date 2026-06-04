package hr.tvz.popovic.deployko.adapter.in;

import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.PortMappings;
import hr.tvz.popovic.deployko.application.port.in.GetServicePortMappingsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServiceRuntimeConfigurationControllerTest {

    @Test
    void gets_port_mappings_and_returns_ok_status() throws Exception {
        PortMappings portMappings = PortMappings.empty()
                .add(new Port(8080), new Port(80))
                .add(new Port(8443, Port.Protocol.UDP), new Port(443, Port.Protocol.UDP));
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new GetServicePortMappingsUseCase.GetServicePortMappingsResult.Success(portMappings)
        ));

        mockMvc.perform(get("/services/{serviceName}/runtime-configuration/port-mappings", "deployko-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hostPort").value(8080))
                .andExpect(jsonPath("$[0].hostProtocol").value("TCP"))
                .andExpect(jsonPath("$[0].containerPort").value(80))
                .andExpect(jsonPath("$[0].containerProtocol").value("TCP"))
                .andExpect(jsonPath("$[1].hostPort").value(8443))
                .andExpect(jsonPath("$[1].hostProtocol").value("UDP"))
                .andExpect(jsonPath("$[1].containerPort").value(443))
                .andExpect(jsonPath("$[1].containerProtocol").value("UDP"));
    }

    @Test
    void returns_not_found_when_getting_port_mappings_for_missing_service() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new GetServicePortMappingsUseCase.GetServicePortMappingsResult.NotFound()
        ));

        mockMvc.perform(get("/services/{serviceName}/runtime-configuration/port-mappings", "missing-api"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_bad_request_when_getting_port_mappings_for_invalid_service_name() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new GetServicePortMappingsUseCase.GetServicePortMappingsResult.Failure()
        ));

        mockMvc.perform(get("/services/{serviceName}/runtime-configuration/port-mappings", "Deployko Api"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_internal_server_error_when_getting_port_mappings_fails_unexpectedly() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new GetServicePortMappingsUseCase.GetServicePortMappingsResult.Failure()
        ));

        mockMvc.perform(get("/services/{serviceName}/runtime-configuration/port-mappings", "deployko-api"))
                .andExpect(status().isInternalServerError());
    }

    private static MockMvc mockMvc(StubServiceRuntimeConfigurationUseCases useCases) {
        return MockMvcBuilders.standaloneSetup(new ServiceRuntimeConfigurationController(useCases)).build();
    }

    private record StubServiceRuntimeConfigurationUseCases(
            GetServicePortMappingsUseCase.GetServicePortMappingsResult getPortMappingsResult
    ) implements GetServicePortMappingsUseCase {

        @Override
        public GetServicePortMappingsResult getServicePortMappings(GetServicePortMappingsCommand command) {
            return getPortMappingsResult;
        }
    }
}
