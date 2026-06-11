package hr.tvz.popovic.deployko.adapter.in;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentAttempt;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentStatus;
import hr.tvz.popovic.deployko.application.domain.model.ImageCommitSha;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.GetDeploymentHistoryUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetLatestDeploymentUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServiceDeploymentControllerTest {

    private static final DeploymentAttempt DEPLOYMENT_ATTEMPT = new DeploymentAttempt(
            new DeploymentId(UUID.fromString("018f4b5d-9c64-7000-9f2e-4d8fbf9f1b22")),
            new ImageVersion("1.0.0"),
            new ImageCommitSha.Known("f5a1c2d"),
            DeploymentStatus.IN_PROGRESS,
            OffsetDateTime.parse("2026-06-07T10:15:30Z")
    );
    private static final DeploymentAttempt OLDER_ATTEMPT = new DeploymentAttempt(
            new DeploymentId(UUID.fromString("018f4b5d-9c64-7000-9f2e-4d8fbf9f1b11")),
            new ImageVersion("0.9.0"),
            new ImageCommitSha.Unknown(),
            DeploymentStatus.SUCCESS,
            OffsetDateTime.parse("2026-06-06T08:00:00Z")
    );

    @Test
    void returns_latest_deployment() throws Exception {
        StubGetLatestDeploymentUseCase useCase = new StubGetLatestDeploymentUseCase(
                new GetLatestDeploymentUseCase.GetLatestDeploymentResult.Found(DEPLOYMENT_ATTEMPT)
        );
        MockMvc mockMvc = mockMvc(useCase, anyHistory());

        mockMvc.perform(get("/services/{serviceName}/deployments/latest", "deployko-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deploymentId").value("018f4b5d-9c64-7000-9f2e-4d8fbf9f1b22"))
                .andExpect(jsonPath("$.imageVersion").value("1.0.0"))
                .andExpect(jsonPath("$.commitSha").value("f5a1c2d"))
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
        ), anyHistory());

        mockMvc.perform(get("/services/{serviceName}/deployments/latest", "deployko-api"))
                .andExpect(status().isNoContent());
    }

    @Test
    void returns_not_found_when_service_is_missing() throws Exception {
        MockMvc mockMvc = mockMvc(new StubGetLatestDeploymentUseCase(
                new GetLatestDeploymentUseCase.GetLatestDeploymentResult.ServiceNotFound()
        ), anyHistory());

        mockMvc.perform(get("/services/{serviceName}/deployments/latest", "missing-api"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_bad_request_when_service_name_is_invalid() throws Exception {
        MockMvc mockMvc = mockMvc(new StubGetLatestDeploymentUseCase(
                new GetLatestDeploymentUseCase.GetLatestDeploymentResult.Found(DEPLOYMENT_ATTEMPT)
        ), anyHistory());

        mockMvc.perform(get("/services/{serviceName}/deployments/latest", "Deployko Api"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_internal_server_error_when_lookup_fails() throws Exception {
        MockMvc mockMvc = mockMvc(new StubGetLatestDeploymentUseCase(
                new GetLatestDeploymentUseCase.GetLatestDeploymentResult.Failure()
        ), anyHistory());

        mockMvc.perform(get("/services/{serviceName}/deployments/latest", "deployko-api"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void returns_deployment_history_ordered_oldest_first() throws Exception {
        StubGetDeploymentHistoryUseCase useCase = new StubGetDeploymentHistoryUseCase(
                new GetDeploymentHistoryUseCase.GetDeploymentHistoryResult.Found(List.of(OLDER_ATTEMPT, DEPLOYMENT_ATTEMPT))
        );
        MockMvc mockMvc = mockMvc(anyLatest(), useCase);

        mockMvc.perform(get("/services/{serviceName}/deployments", "deployko-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].imageVersion").value("0.9.0"))
                .andExpect(jsonPath("$[0].commitSha").doesNotExist())
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[1].imageVersion").value("1.0.0"))
                .andExpect(jsonPath("$[1].commitSha").value("f5a1c2d"))
                .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"));

        assertThat(useCase.command).isEqualTo(new GetDeploymentHistoryUseCase.GetDeploymentHistoryCommand(
                new ServiceName("deployko-api"),
                Optional.empty()
        ));
    }

    @Test
    void passes_since_to_deployment_history_use_case() throws Exception {
        StubGetDeploymentHistoryUseCase useCase = new StubGetDeploymentHistoryUseCase(
                new GetDeploymentHistoryUseCase.GetDeploymentHistoryResult.Found(List.of())
        );
        MockMvc mockMvc = mockMvc(anyLatest(), useCase);

        mockMvc.perform(get("/services/{serviceName}/deployments", "deployko-api")
                        .param("since", "2026-06-07T10:15:30Z"))
                .andExpect(status().isOk());

        assertThat(useCase.command).isEqualTo(new GetDeploymentHistoryUseCase.GetDeploymentHistoryCommand(
                new ServiceName("deployko-api"),
                Optional.of(OffsetDateTime.parse("2026-06-07T10:15:30Z"))
        ));
    }

    @Test
    void returns_not_found_when_history_service_is_missing() throws Exception {
        MockMvc mockMvc = mockMvc(anyLatest(), new StubGetDeploymentHistoryUseCase(
                new GetDeploymentHistoryUseCase.GetDeploymentHistoryResult.ServiceNotFound()
        ));

        mockMvc.perform(get("/services/{serviceName}/deployments", "missing-api"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_bad_request_when_history_service_name_is_invalid() throws Exception {
        MockMvc mockMvc = mockMvc(anyLatest(), new StubGetDeploymentHistoryUseCase(
                new GetDeploymentHistoryUseCase.GetDeploymentHistoryResult.Found(List.of())
        ));

        mockMvc.perform(get("/services/{serviceName}/deployments", "Deployko Api"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_bad_request_when_since_is_not_a_timestamp() throws Exception {
        MockMvc mockMvc = mockMvc(anyLatest(), new StubGetDeploymentHistoryUseCase(
                new GetDeploymentHistoryUseCase.GetDeploymentHistoryResult.Found(List.of())
        ));

        mockMvc.perform(get("/services/{serviceName}/deployments", "deployko-api")
                        .param("since", "not-a-timestamp"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_internal_server_error_when_history_lookup_fails() throws Exception {
        MockMvc mockMvc = mockMvc(anyLatest(), new StubGetDeploymentHistoryUseCase(
                new GetDeploymentHistoryUseCase.GetDeploymentHistoryResult.Failure()
        ));

        mockMvc.perform(get("/services/{serviceName}/deployments", "deployko-api"))
                .andExpect(status().isInternalServerError());
    }

    private static MockMvc mockMvc(
            GetLatestDeploymentUseCase getLatestDeploymentUseCase,
            GetDeploymentHistoryUseCase getDeploymentHistoryUseCase
    ) {
        return MockMvcBuilders
                .standaloneSetup(new ServiceDeploymentController(getLatestDeploymentUseCase, getDeploymentHistoryUseCase))
                .build();
    }

    private static GetLatestDeploymentUseCase anyLatest() {
        return new StubGetLatestDeploymentUseCase(
                new GetLatestDeploymentUseCase.GetLatestDeploymentResult.Found(DEPLOYMENT_ATTEMPT)
        );
    }

    private static GetDeploymentHistoryUseCase anyHistory() {
        return new StubGetDeploymentHistoryUseCase(
                new GetDeploymentHistoryUseCase.GetDeploymentHistoryResult.Found(List.of())
        );
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

    private static final class StubGetDeploymentHistoryUseCase implements GetDeploymentHistoryUseCase {

        private final GetDeploymentHistoryResult result;
        private GetDeploymentHistoryCommand command;

        private StubGetDeploymentHistoryUseCase(GetDeploymentHistoryResult result) {
            this.result = result;
        }

        @Override
        public GetDeploymentHistoryResult getDeploymentHistory(GetDeploymentHistoryCommand command) {
            this.command = command;
            return result;
        }
    }
}
