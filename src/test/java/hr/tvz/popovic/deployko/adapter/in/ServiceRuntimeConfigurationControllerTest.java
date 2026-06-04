package hr.tvz.popovic.deployko.adapter.in;

import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.PortMappings;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMounts;
import hr.tvz.popovic.deployko.application.port.in.CreateServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.CreateServiceVolumeMountUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServicePortMappingsUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceVolumeMountsUseCase;
import hr.tvz.popovic.deployko.application.port.in.UpdateServiceVolumeMountUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Test
    void gets_volume_mounts_and_returns_ok_status() throws Exception {
        VolumeMounts volumeMounts = VolumeMounts.empty()
                .add(new VolumeMount.BindMount(
                        new VolumeMount.HostPath("/opt/deployko/config"),
                        new VolumeMount.Target("/app/config"),
                        true
                ))
                .add(new VolumeMount.NamedVolumeMount(
                        new VolumeMount.VolumeName("deployko_data"),
                        new VolumeMount.Target("/var/lib/deployko"),
                        false
                ));
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult.Success(volumeMounts)
        ));

        mockMvc.perform(get("/services/{serviceName}/runtime-configuration/volume-mounts", "deployko-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetPath").value("/app/config"))
                .andExpect(jsonPath("$[0].mountType").value("BIND"))
                .andExpect(jsonPath("$[0].source").value("/opt/deployko/config"))
                .andExpect(jsonPath("$[0].readOnly").value(true))
                .andExpect(jsonPath("$[1].targetPath").value("/var/lib/deployko"))
                .andExpect(jsonPath("$[1].mountType").value("VOLUME"))
                .andExpect(jsonPath("$[1].source").value("deployko_data"))
                .andExpect(jsonPath("$[1].readOnly").value(false));
    }

    @Test
    void returns_not_found_when_getting_volume_mounts_for_missing_service() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult.NotFound()
        ));

        mockMvc.perform(get("/services/{serviceName}/runtime-configuration/volume-mounts", "missing-api"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_bad_request_when_getting_volume_mounts_for_invalid_service_name() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult.Failure()
        ));

        mockMvc.perform(get("/services/{serviceName}/runtime-configuration/volume-mounts", "Deployko Api"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_internal_server_error_when_getting_volume_mounts_fails_unexpectedly() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult.Failure()
        ));

        mockMvc.perform(get("/services/{serviceName}/runtime-configuration/volume-mounts", "deployko-api"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void creates_volume_mount_and_returns_created_status() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.Success()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime-configuration/volume-mounts", "deployko-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateVolumeMountRequest()))
                .andExpect(status().isCreated());
    }

    @Test
    void returns_not_found_when_creating_volume_mount_for_missing_service() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.ServiceNotFound()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime-configuration/volume-mounts", "missing-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateVolumeMountRequest()))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_conflict_when_creating_duplicate_volume_mount() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.AlreadyExists()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime-configuration/volume-mounts", "deployko-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateVolumeMountRequest()))
                .andExpect(status().isConflict());
    }

    @Test
    void returns_bad_request_when_creating_volume_mount_with_invalid_request() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.Failure()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime-configuration/volume-mounts", "deployko-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetPath": "app/config",
                                  "mountType": "BIND",
                                  "source": "/opt/deployko/config",
                                  "readOnly": true
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_bad_request_when_creating_volume_mount_with_invalid_mount_type() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.Failure()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime-configuration/volume-mounts", "deployko-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetPath": "/app/config",
                                  "mountType": "TMPFS",
                                  "source": "/opt/deployko/config",
                                  "readOnly": true
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_internal_server_error_when_creating_volume_mount_fails_unexpectedly() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.Failure()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime-configuration/volume-mounts", "deployko-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateVolumeMountRequest()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updates_volume_mount_and_returns_no_content() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.Success()
        ));

        mockMvc.perform(put("/services/{serviceName}/runtime-configuration/volume-mounts", "deployko-api")
                        .queryParam("targetPath", "/app/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateVolumeMountRequest()))
                .andExpect(status().isNoContent());
    }

    @Test
    void returns_not_found_when_updating_volume_mount_for_missing_service() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.ServiceNotFound()
        ));

        mockMvc.perform(put("/services/{serviceName}/runtime-configuration/volume-mounts", "missing-api")
                        .queryParam("targetPath", "/app/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateVolumeMountRequest()))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_not_found_when_updating_missing_volume_mount() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.VolumeMountNotFound()
        ));

        mockMvc.perform(put("/services/{serviceName}/runtime-configuration/volume-mounts", "deployko-api")
                        .queryParam("targetPath", "/app/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateVolumeMountRequest()))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_bad_request_when_updating_volume_mount_with_invalid_target_path() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.Failure()
        ));

        mockMvc.perform(put("/services/{serviceName}/runtime-configuration/volume-mounts", "deployko-api")
                        .queryParam("targetPath", "app/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateVolumeMountRequest()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_bad_request_when_updating_volume_mount_with_invalid_request() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.Failure()
        ));

        mockMvc.perform(put("/services/{serviceName}/runtime-configuration/volume-mounts", "deployko-api")
                        .queryParam("targetPath", "/app/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mountType": "VOLUME",
                                  "source": " deployko data ",
                                  "readOnly": false
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_internal_server_error_when_updating_volume_mount_fails_unexpectedly() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeConfigurationUseCases(
                new UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.Failure()
        ));

        mockMvc.perform(put("/services/{serviceName}/runtime-configuration/volume-mounts", "deployko-api")
                        .queryParam("targetPath", "/app/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateVolumeMountRequest()))
                .andExpect(status().isInternalServerError());
    }

    private static MockMvc mockMvc(StubServiceRuntimeConfigurationUseCases useCases) {
        return MockMvcBuilders
                .standaloneSetup(new ServiceRuntimeConfigurationController(
                        useCases,
                        useCases,
                        useCases,
                        useCases,
                        useCases,
                        useCases
                ))
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

    private static String validCreateVolumeMountRequest() {
        return """
                {
                  "targetPath": "/app/config",
                  "mountType": "BIND",
                  "source": "/opt/deployko/config",
                  "readOnly": true
                }
                """;
    }

    private static String validUpdateVolumeMountRequest() {
        return """
                {
                  "mountType": "VOLUME",
                  "source": "deployko_data",
                  "readOnly": false
                }
                """;
    }

    private record StubServiceRuntimeConfigurationUseCases(
            GetServicePortMappingsUseCase.GetServicePortMappingsResult getPortMappingsResult,
            CreateServicePortMappingUseCase.CreateServicePortMappingResult createPortMappingResult,
            DeleteServicePortMappingUseCase.DeleteServicePortMappingResult deletePortMappingResult,
            GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult getVolumeMountsResult,
            CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult createVolumeMountResult,
            UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult updateVolumeMountResult
    ) implements GetServicePortMappingsUseCase, CreateServicePortMappingUseCase, DeleteServicePortMappingUseCase,
            GetServiceVolumeMountsUseCase, CreateServiceVolumeMountUseCase, UpdateServiceVolumeMountUseCase {

        private StubServiceRuntimeConfigurationUseCases(
                GetServicePortMappingsUseCase.GetServicePortMappingsResult getPortMappingsResult
        ) {
            this(
                    getPortMappingsResult,
                    new CreateServicePortMappingUseCase.CreateServicePortMappingResult.Failure(),
                    new DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.Failure(),
                    new GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult.Failure(),
                    new CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.Failure(),
                    new UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.Failure()
            );
        }

        private StubServiceRuntimeConfigurationUseCases(
                GetServicePortMappingsUseCase.GetServicePortMappingsResult getPortMappingsResult,
                CreateServicePortMappingUseCase.CreateServicePortMappingResult createPortMappingResult
        ) {
            this(
                    getPortMappingsResult,
                    createPortMappingResult,
                    new DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.Failure(),
                    new GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult.Failure(),
                    new CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.Failure(),
                    new UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.Failure()
            );
        }

        private StubServiceRuntimeConfigurationUseCases(
                DeleteServicePortMappingUseCase.DeleteServicePortMappingResult deletePortMappingResult
        ) {
            this(
                    new GetServicePortMappingsUseCase.GetServicePortMappingsResult.Failure(),
                    new CreateServicePortMappingUseCase.CreateServicePortMappingResult.Failure(),
                    deletePortMappingResult,
                    new GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult.Failure(),
                    new CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.Failure(),
                    new UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.Failure()
            );
        }

        private StubServiceRuntimeConfigurationUseCases(
                GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult getVolumeMountsResult
        ) {
            this(
                    new GetServicePortMappingsUseCase.GetServicePortMappingsResult.Failure(),
                    new CreateServicePortMappingUseCase.CreateServicePortMappingResult.Failure(),
                    new DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.Failure(),
                    getVolumeMountsResult,
                    new CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.Failure(),
                    new UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.Failure()
            );
        }

        private StubServiceRuntimeConfigurationUseCases(
                CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult createVolumeMountResult
        ) {
            this(
                    new GetServicePortMappingsUseCase.GetServicePortMappingsResult.Failure(),
                    new CreateServicePortMappingUseCase.CreateServicePortMappingResult.Failure(),
                    new DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.Failure(),
                    new GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult.Failure(),
                    createVolumeMountResult,
                    new UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.Failure()
            );
        }

        private StubServiceRuntimeConfigurationUseCases(
                UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult updateVolumeMountResult
        ) {
            this(
                    new GetServicePortMappingsUseCase.GetServicePortMappingsResult.Failure(),
                    new CreateServicePortMappingUseCase.CreateServicePortMappingResult.Failure(),
                    new DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.Failure(),
                    new GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult.Failure(),
                    new CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.Failure(),
                    updateVolumeMountResult
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

        @Override
        public GetServiceVolumeMountsResult getServiceVolumeMounts(GetServiceVolumeMountsCommand command) {
            return getVolumeMountsResult;
        }

        @Override
        public CreateServiceVolumeMountResult createServiceVolumeMount(CreateServiceVolumeMountCommand command) {
            return createVolumeMountResult;
        }

        @Override
        public UpdateServiceVolumeMountResult updateServiceVolumeMount(UpdateServiceVolumeMountCommand command) {
            return updateVolumeMountResult;
        }
    }
}
