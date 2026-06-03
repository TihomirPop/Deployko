package hr.tvz.popovic.deployko.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.ServiceDeploymentUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ServiceDeploymentControllerTest {

    @Test
    void deploys_service_and_returns_no_content() throws Exception {
        StubServiceDeploymentUseCase useCase = new StubServiceDeploymentUseCase(
                new ServiceDeploymentUseCase.DeployServiceResult.Success()
        );
        MockMvc mockMvc = mockMvc(useCase);

        mockMvc.perform(post("/services/{serviceName}/deployments", "deployko-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageVersion": "1.0.0"
                                }
                                """))
                .andExpect(status().isNoContent());

        assertThat(useCase.deployServiceCommand).isEqualTo(new ServiceDeploymentUseCase.DeployServiceCommand(
                new ServiceName("deployko-api"),
                new ImageVersion("1.0.0")
        ));
    }

    @Test
    void returns_not_found_when_deploying_missing_service() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceDeploymentUseCase(
                new ServiceDeploymentUseCase.DeployServiceResult.ServiceNotFound()
        ));

        mockMvc.perform(post("/services/{serviceName}/deployments", "missing-service")
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
        MockMvc mockMvc = mockMvc(new StubServiceDeploymentUseCase(
                new ServiceDeploymentUseCase.DeployServiceResult.Success()
        ));

        mockMvc.perform(post("/services/{serviceName}/deployments", "Deployko Api")
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
        MockMvc mockMvc = mockMvc(new StubServiceDeploymentUseCase(
                new ServiceDeploymentUseCase.DeployServiceResult.Success()
        ));

        mockMvc.perform(post("/services/{serviceName}/deployments", "deployko-api")
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
        MockMvc mockMvc = mockMvc(new StubServiceDeploymentUseCase(
                new ServiceDeploymentUseCase.DeployServiceResult.DesiredStateFailure()
        ));

        mockMvc.perform(post("/services/{serviceName}/deployments", "deployko-api")
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
        MockMvc mockMvc = mockMvc(new StubServiceDeploymentUseCase(
                new ServiceDeploymentUseCase.DeployServiceResult.DockerFailure()
        ));

        mockMvc.perform(post("/services/{serviceName}/deployments", "deployko-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageVersion": "1.0.0"
                                }
                                """))
                .andExpect(status().isInternalServerError());
    }

    private static MockMvc mockMvc(ServiceDeploymentUseCase serviceDeploymentUseCase) {
        return MockMvcBuilders.standaloneSetup(new ServiceDeploymentController(serviceDeploymentUseCase)).build();
    }

    private static final class StubServiceDeploymentUseCase implements ServiceDeploymentUseCase {

        private final DeployServiceResult deployServiceResult;
        private DeployServiceCommand deployServiceCommand;

        private StubServiceDeploymentUseCase(DeployServiceResult deployServiceResult) {
            this.deployServiceResult = deployServiceResult;
        }

        @Override
        public DeployServiceResult deployService(DeployServiceCommand command) {
            this.deployServiceCommand = command;
            return deployServiceResult;
        }

        @Override
        public StartServiceResult startService(StartServiceCommand command) {
            throw new UnsupportedOperationException("start service is not implemented yet");
        }

        @Override
        public StopServiceResult stopService(StopServiceCommand command) {
            throw new UnsupportedOperationException("stop service is not implemented yet");
        }
    }
}
