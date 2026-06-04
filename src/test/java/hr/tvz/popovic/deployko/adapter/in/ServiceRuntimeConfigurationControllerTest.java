package hr.tvz.popovic.deployko.adapter.in;

import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.PortMappings;
import hr.tvz.popovic.deployko.application.port.in.CreateServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServicePortMappingsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void creates_port_mapping_and_returns_created_status() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new GetServicePortMappingsUseCase.GetServicePortMappingsResult.Failure(),
                new CreateServicePortMappingUseCase.CreateServicePortMappingResult.Success()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime-configuration/port-mappings", "deployko-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostPort": 8080,
                                  "hostProtocol": "TCP",
                                  "containerPort": 80,
                                  "containerProtocol": "TCP"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void returns_not_found_when_creating_port_mapping_for_missing_service() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new GetServicePortMappingsUseCase.GetServicePortMappingsResult.Failure(),
                new CreateServicePortMappingUseCase.CreateServicePortMappingResult.ServiceNotFound()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime-configuration/port-mappings", "missing-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_conflict_when_creating_duplicate_port_mapping() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new GetServicePortMappingsUseCase.GetServicePortMappingsResult.Failure(),
                new CreateServicePortMappingUseCase.CreateServicePortMappingResult.AlreadyExists()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime-configuration/port-mappings", "deployko-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isConflict());
    }

    @Test
    void returns_bad_request_when_creating_port_mapping_with_invalid_request() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new GetServicePortMappingsUseCase.GetServicePortMappingsResult.Failure(),
                new CreateServicePortMappingUseCase.CreateServicePortMappingResult.Failure()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime-configuration/port-mappings", "deployko-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostPort": 70000,
                                  "hostProtocol": "TCP",
                                  "containerPort": 80,
                                  "containerProtocol": "TCP"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_bad_request_when_creating_port_mapping_with_invalid_protocol() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new GetServicePortMappingsUseCase.GetServicePortMappingsResult.Failure(),
                new CreateServicePortMappingUseCase.CreateServicePortMappingResult.Failure()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime-configuration/port-mappings", "deployko-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostPort": 8080,
                                  "hostProtocol": "HTTP",
                                  "containerPort": 80,
                                  "containerProtocol": "TCP"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_internal_server_error_when_creating_port_mapping_fails_unexpectedly() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new GetServicePortMappingsUseCase.GetServicePortMappingsResult.Failure(),
                new CreateServicePortMappingUseCase.CreateServicePortMappingResult.Failure()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime-configuration/port-mappings", "deployko-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void deletes_port_mapping_and_returns_no_content() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.Success()
        ));

        mockMvc.perform(delete(
                        "/services/{serviceName}/runtime-configuration/port-mappings/{hostProtocol}/{hostPort}",
                        "deployko-api",
                        "TCP",
                        8080
                ))
                .andExpect(status().isNoContent());
    }

    @Test
    void returns_not_found_when_deleting_port_mapping_for_missing_service() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.ServiceNotFound()
        ));

        mockMvc.perform(delete(
                        "/services/{serviceName}/runtime-configuration/port-mappings/{hostProtocol}/{hostPort}",
                        "missing-api",
                        "TCP",
                        8080
                ))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_not_found_when_deleting_missing_port_mapping() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.PortMappingNotFound()
        ));

        mockMvc.perform(delete(
                        "/services/{serviceName}/runtime-configuration/port-mappings/{hostProtocol}/{hostPort}",
                        "deployko-api",
                        "TCP",
                        8081
                ))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_bad_request_when_deleting_port_mapping_with_invalid_protocol() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.Failure()
        ));

        mockMvc.perform(delete(
                        "/services/{serviceName}/runtime-configuration/port-mappings/{hostProtocol}/{hostPort}",
                        "deployko-api",
                        "HTTP",
                        8080
                ))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_bad_request_when_deleting_port_mapping_with_invalid_port() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.Failure()
        ));

        mockMvc.perform(delete(
                        "/services/{serviceName}/runtime-configuration/port-mappings/{hostProtocol}/{hostPort}",
                        "deployko-api",
                        "TCP",
                        70000
                ))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_internal_server_error_when_deleting_port_mapping_fails_unexpectedly() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.Failure()
        ));

        mockMvc.perform(delete(
                        "/services/{serviceName}/runtime-configuration/port-mappings/{hostProtocol}/{hostPort}",
                        "deployko-api",
                        "TCP",
                        8080
                ))
                .andExpect(status().isInternalServerError());
    }

    private static MockMvc mockMvc(StubServiceRuntimeConfigurationUseCases useCases) {
        return MockMvcBuilders
                .standaloneSetup(new ServiceRuntimeConfigurationController(useCases, useCases, useCases))
                .build();
    }

    private static String validCreateRequest() {
        return """
                {
                  "hostPort": 8080,
                  "hostProtocol": "TCP",
                  "containerPort": 80,
                  "containerProtocol": "TCP"
                }
                """;
    }

    private record StubServiceRuntimeConfigurationUseCases(
            GetServicePortMappingsUseCase.GetServicePortMappingsResult getPortMappingsResult,
            CreateServicePortMappingUseCase.CreateServicePortMappingResult createPortMappingResult,
            DeleteServicePortMappingUseCase.DeleteServicePortMappingResult deletePortMappingResult
    ) implements GetServicePortMappingsUseCase, CreateServicePortMappingUseCase, DeleteServicePortMappingUseCase {

        private StubServiceRuntimeConfigurationUseCases(
                GetServicePortMappingsUseCase.GetServicePortMappingsResult getPortMappingsResult
        ) {
            this(
                    getPortMappingsResult,
                    new CreateServicePortMappingUseCase.CreateServicePortMappingResult.Failure(),
                    new DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.Failure()
            );
        }

        private StubServiceRuntimeConfigurationUseCases(
                GetServicePortMappingsUseCase.GetServicePortMappingsResult getPortMappingsResult,
                CreateServicePortMappingUseCase.CreateServicePortMappingResult createPortMappingResult
        ) {
            this(
                    getPortMappingsResult,
                    createPortMappingResult,
                    new DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.Failure()
            );
        }

        private StubServiceRuntimeConfigurationUseCases(
                DeleteServicePortMappingUseCase.DeleteServicePortMappingResult deletePortMappingResult
        ) {
            this(
                    new GetServicePortMappingsUseCase.GetServicePortMappingsResult.Failure(),
                    new CreateServicePortMappingUseCase.CreateServicePortMappingResult.Failure(),
                    deletePortMappingResult
            );
        }

        @Override
        public GetServicePortMappingsResult getServicePortMappings(GetServicePortMappingsCommand command) {
            return getPortMappingsResult;
        }

        @Override
        public CreateServicePortMappingResult createServicePortMapping(CreateServicePortMappingCommand command) {
            return createPortMappingResult;
        }

        @Override
        public DeleteServicePortMappingResult deleteServicePortMapping(DeleteServicePortMappingCommand command) {
            return deletePortMappingResult;
        }
    }
}
