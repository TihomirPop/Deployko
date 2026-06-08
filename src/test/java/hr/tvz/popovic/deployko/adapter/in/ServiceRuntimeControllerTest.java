package hr.tvz.popovic.deployko.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.ServiceRuntimeStatus;
import hr.tvz.popovic.deployko.application.port.in.DeployServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceRuntimeStatusUseCase;
import hr.tvz.popovic.deployko.application.port.in.StartServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.StopServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.UninstallServiceUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ServiceRuntimeControllerTest {

    @Test
    void deploys_service_and_returns_no_content() throws Exception {
        StubServiceRuntimeUseCases useCases = new StubServiceRuntimeUseCases(
                new DeployServiceUseCase.DeployServiceResult.Success()
        );
        MockMvc mockMvc = mockMvc(useCases);

        mockMvc.perform(post("/services/{serviceName}/runtime/deploy", "deployko-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageVersion": "1.0.0"
                                }
                                """))
                .andExpect(status().isNoContent());

        assertThat(useCases.deployServiceCommand).isEqualTo(new DeployServiceUseCase.DeployServiceCommand(
                new ServiceName("deployko-api"),
                new ImageVersion("1.0.0")
        ));
    }

    @Test
    void returns_not_found_when_deploying_missing_service() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeUseCases(
                new DeployServiceUseCase.DeployServiceResult.ServiceNotFound()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime/deploy", "missing-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageVersion": "1.0.0"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_not_found_when_deployment_image_is_missing() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeUseCases(
                new DeployServiceUseCase.DeployServiceResult.ImageNotFound()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime/deploy", "deployko-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageVersion": "1.0.0"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_bad_request_when_service_name_is_invalid() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeUseCases(
                new DeployServiceUseCase.DeployServiceResult.Success()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime/deploy", "Deployko Api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageVersion": "1.0.0"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_bad_request_when_image_version_is_invalid() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeUseCases(
                new DeployServiceUseCase.DeployServiceResult.Success()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime/deploy", "deployko-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageVersion": "feature/build"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_internal_server_error_when_desired_state_update_fails() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeUseCases(
                new DeployServiceUseCase.DeployServiceResult.DesiredStateFailure()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime/deploy", "deployko-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageVersion": "1.0.0"
                                }
                                """))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void returns_internal_server_error_when_docker_deployment_fails() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeUseCases(
                new DeployServiceUseCase.DeployServiceResult.DockerFailure()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime/deploy", "deployko-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageVersion": "1.0.0"
                                }
                                """))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void returns_runtime_status() throws Exception {
        StubServiceRuntimeUseCases useCases = new StubServiceRuntimeUseCases(
                new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult.Success(ServiceRuntimeStatus.RUNNING)
        );
        MockMvc mockMvc = mockMvc(useCases);

        mockMvc.perform(get("/services/{serviceName}/runtime/status", "deployko-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));

        assertThat(useCases.getServiceRuntimeStatusCommand)
                .isEqualTo(new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusCommand(
                        new ServiceName("deployko-api")
                ));
    }

    @Test
    void returns_not_found_when_runtime_status_service_is_missing() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeUseCases(
                new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult.ServiceNotFound()
        ));

        mockMvc.perform(get("/services/{serviceName}/runtime/status", "missing-service"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_bad_request_when_runtime_status_service_name_is_invalid() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeUseCases(
                new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult.Success(ServiceRuntimeStatus.RUNNING)
        ));

        mockMvc.perform(get("/services/{serviceName}/runtime/status", "Deployko Api"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_internal_server_error_when_runtime_status_desired_state_lookup_fails() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeUseCases(
                new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult.DesiredStateFailure()
        ));

        mockMvc.perform(get("/services/{serviceName}/runtime/status", "deployko-api"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void returns_internal_server_error_when_runtime_status_docker_lookup_fails() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeUseCases(
                new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult.DockerFailure()
        ));

        mockMvc.perform(get("/services/{serviceName}/runtime/status", "deployko-api"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void uninstalls_service_and_returns_no_content() throws Exception {
        StubServiceRuntimeUseCases useCases = new StubServiceRuntimeUseCases(
                new UninstallServiceUseCase.UninstallServiceResult.Success()
        );
        MockMvc mockMvc = mockMvc(useCases);

        mockMvc.perform(post("/services/{serviceName}/runtime/uninstall", "deployko-api"))
                .andExpect(status().isNoContent());

        assertThat(useCases.uninstallServiceCommand)
                .isEqualTo(new UninstallServiceUseCase.UninstallServiceCommand(new ServiceName("deployko-api")));
    }

    @Test
    void returns_not_found_when_uninstalling_missing_or_undeployed_service() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeUseCases(
                new UninstallServiceUseCase.UninstallServiceResult.NotDeployed()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime/uninstall", "deployko-api"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_conflict_when_uninstall_detects_drift() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeUseCases(
                new UninstallServiceUseCase.UninstallServiceResult.Drift()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime/uninstall", "deployko-api"))
                .andExpect(status().isConflict());
    }

    @Test
    void returns_bad_request_when_uninstall_service_name_is_invalid() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeUseCases(
                new UninstallServiceUseCase.UninstallServiceResult.Success()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime/uninstall", "Deployko Api"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_internal_server_error_when_uninstall_fails() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceRuntimeUseCases(
                new UninstallServiceUseCase.UninstallServiceResult.DockerFailure()
        ));

        mockMvc.perform(post("/services/{serviceName}/runtime/uninstall", "deployko-api"))
                .andExpect(status().isInternalServerError());
    }

    private static MockMvc mockMvc(StubServiceRuntimeUseCases useCases) {
        return MockMvcBuilders.standaloneSetup(new ServiceRuntimeController(useCases, useCases, useCases, useCases, useCases))
                .build();
    }

    private static final class StubServiceRuntimeUseCases
            implements DeployServiceUseCase, StartServiceUseCase, StopServiceUseCase, UninstallServiceUseCase,
            GetServiceRuntimeStatusUseCase {

        private final DeployServiceResult deployServiceResult;
        private final UninstallServiceResult uninstallServiceResult;
        private final GetServiceRuntimeStatusResult getServiceRuntimeStatusResult;
        private DeployServiceCommand deployServiceCommand;
        private UninstallServiceCommand uninstallServiceCommand;
        private GetServiceRuntimeStatusCommand getServiceRuntimeStatusCommand;

        private StubServiceRuntimeUseCases(DeployServiceResult deployServiceResult) {
            this.deployServiceResult = deployServiceResult;
            this.uninstallServiceResult = new UninstallServiceResult.Success();
            this.getServiceRuntimeStatusResult = new GetServiceRuntimeStatusResult.Success(ServiceRuntimeStatus.RUNNING);
        }

        private StubServiceRuntimeUseCases(GetServiceRuntimeStatusResult getServiceRuntimeStatusResult) {
            this.deployServiceResult = new DeployServiceResult.Success();
            this.uninstallServiceResult = new UninstallServiceResult.Success();
            this.getServiceRuntimeStatusResult = getServiceRuntimeStatusResult;
        }

        private StubServiceRuntimeUseCases(UninstallServiceResult uninstallServiceResult) {
            this.deployServiceResult = new DeployServiceResult.Success();
            this.uninstallServiceResult = uninstallServiceResult;
            this.getServiceRuntimeStatusResult = new GetServiceRuntimeStatusResult.Success(ServiceRuntimeStatus.RUNNING);
        }

        @Override
        public DeployServiceResult deployService(DeployServiceCommand command) {
            this.deployServiceCommand = command;
            return deployServiceResult;
        }

        @Override
        public StartServiceResult startService(StartServiceCommand command) {
            return new StartServiceResult.Success();
        }

        @Override
        public StopServiceResult stopService(StopServiceCommand command) {
            return new StopServiceResult.Success();
        }

        @Override
        public UninstallServiceResult uninstallService(UninstallServiceCommand command) {
            this.uninstallServiceCommand = command;
            return uninstallServiceResult;
        }

        @Override
        public GetServiceRuntimeStatusResult getServiceRuntimeStatus(GetServiceRuntimeStatusCommand command) {
            this.getServiceRuntimeStatusCommand = command;
            return getServiceRuntimeStatusResult;
        }
    }
}
