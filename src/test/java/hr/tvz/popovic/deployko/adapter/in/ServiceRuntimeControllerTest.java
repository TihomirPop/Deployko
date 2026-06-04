package hr.tvz.popovic.deployko.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.DeployServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.StartServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.StopServiceUseCase;
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

    private static MockMvc mockMvc(StubServiceRuntimeUseCases useCases) {
        return MockMvcBuilders.standaloneSetup(new ServiceRuntimeController(useCases, useCases, useCases)).build();
    }

    private static final class StubServiceRuntimeUseCases
            implements DeployServiceUseCase, StartServiceUseCase, StopServiceUseCase {

        private final DeployServiceResult deployServiceResult;
        private DeployServiceCommand deployServiceCommand;

        private StubServiceRuntimeUseCases(DeployServiceResult deployServiceResult) {
            this.deployServiceResult = deployServiceResult;
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
    }
}
