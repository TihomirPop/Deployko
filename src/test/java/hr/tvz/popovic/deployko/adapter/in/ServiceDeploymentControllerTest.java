package hr.tvz.popovic.deployko.adapter.in;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentAttempt;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentStatus;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.GetLatestDeploymentUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServiceDeploymentControllerTest {

    private static final DeploymentAttempt DEPLOYMENT_ATTEMPT = new DeploymentAttempt(
            new DeploymentId(UUID.fromString("018f4b5d-9c64-7000-9f2e-4d8fbf9f1b22")),
            new ImageVersion("1.0.0"),
            DeploymentStatus.IN_PROGRESS,
            OffsetDateTime.parse("2026-06-07T10:15:30Z")
    );

    @Test
    void returns_latest_deployment() throws Exception {
        StubGetLatestDeploymentUseCase useCase = new StubGetLatestDeploymentUseCase(
                new GetLatestDeploymentUseCase.GetLatestDeploymentResult.Found(DEPLOYMENT_ATTEMPT)
        );
        MockMvc mockMvc = mockMvc(useCase);

        mockMvc.perform(get("/services/{serviceName}/deployments/latest", "deployko-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deploymentId").value("018f4b5d-9c64-7000-9f2e-4d8fbf9f1b22"))
                .andExpect(jsonPath("$.imageVersion").value("1.0.0"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.recordedAt").value("2026-06-07T10:15:30Z"));

        assertThat(useCase.command).isEqualTo(new GetLatestDeploymentUseCase.GetLatestDeploymentCommand(
                new ServiceName("deployko-api")
        ));
    }

    @Test
    void returns_no_content_when_service_has_no_deployment_history() throws Exception {
        MockMvc mockMvc = mockMvc(new StubGetLatestDeploymentUseCase(
                new GetLatestDeploymentUseCase.GetLatestDeploymentResult.NotDeployed()
        ));

        mockMvc.perform(get("/services/{serviceName}/deployments/latest", "deployko-api"))
                .andExpect(status().isNoContent());
    }

    @Test
    void returns_not_found_when_service_is_missing() throws Exception {
        MockMvc mockMvc = mockMvc(new StubGetLatestDeploymentUseCase(
                new GetLatestDeploymentUseCase.GetLatestDeploymentResult.ServiceNotFound()
        ));

        mockMvc.perform(get("/services/{serviceName}/deployments/latest", "missing-api"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_bad_request_when_service_name_is_invalid() throws Exception {
        MockMvc mockMvc = mockMvc(new StubGetLatestDeploymentUseCase(
                new GetLatestDeploymentUseCase.GetLatestDeploymentResult.Found(DEPLOYMENT_ATTEMPT)
        ));

        mockMvc.perform(get("/services/{serviceName}/deployments/latest", "Deployko Api"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_internal_server_error_when_lookup_fails() throws Exception {
        MockMvc mockMvc = mockMvc(new StubGetLatestDeploymentUseCase(
                new GetLatestDeploymentUseCase.GetLatestDeploymentResult.Failure()
        ));

        mockMvc.perform(get("/services/{serviceName}/deployments/latest", "deployko-api"))
                .andExpect(status().isInternalServerError());
    }

    private static MockMvc mockMvc(GetLatestDeploymentUseCase getLatestDeploymentUseCase) {
        return MockMvcBuilders
                .standaloneSetup(new ServiceDeploymentController(getLatestDeploymentUseCase))
                .build();
    }

    private static final class StubGetLatestDeploymentUseCase implements GetLatestDeploymentUseCase {

        private final GetLatestDeploymentResult result;
        private GetLatestDeploymentCommand command;

        private StubGetLatestDeploymentUseCase(GetLatestDeploymentResult result) {
            this.result = result;
        }

        @Override
        public GetLatestDeploymentResult getLatestDeployment(GetLatestDeploymentCommand command) {
            this.command = command;
            return result;
        }
    }
}
