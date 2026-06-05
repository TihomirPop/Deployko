package hr.tvz.popovic.deployko.adapter.in;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.RuntimeConfiguration;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.ServiceRuntimeStatus;
import hr.tvz.popovic.deployko.application.domain.model.ServiceSummary;
import hr.tvz.popovic.deployko.application.port.in.CreateServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceSummariesUseCase;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ServiceControllerTest {

    @Test
    void creates_service_and_returns_created_status() throws Exception {
        Service service = new Service(
                new ServiceName("deployko-api"),
                new ImageRepository("ghcr.io/deployko/api"),
                RuntimeConfiguration.empty()
        );
        MockMvc mockMvc = mockMvc(new StubServiceUseCases(
                new GetServiceSummariesUseCase.GetServiceSummariesResult.Success(List.of()),
                new CreateServiceUseCase.CreateServiceResult.Success(service),
                new DeleteServiceUseCase.DeleteServiceResult.Success()
        ));

        mockMvc.perform(post("/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "deployko-api",
                                  "imageRepository": "ghcr.io/deployko/api"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("deployko-api"))
                .andExpect(jsonPath("$.imageRepository").value("ghcr.io/deployko/api"));
    }

    @Test
    void returns_conflict_when_service_name_already_exists() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceUseCases(
                new GetServiceSummariesUseCase.GetServiceSummariesResult.Success(List.of()),
                new CreateServiceUseCase.CreateServiceResult.DuplicateServiceName(),
                new DeleteServiceUseCase.DeleteServiceResult.Success()
        ));

        mockMvc.perform(post("/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "deployko-api",
                                  "imageRepository": "ghcr.io/deployko/api"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void returns_bad_request_when_create_request_is_invalid() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceUseCases(
                new GetServiceSummariesUseCase.GetServiceSummariesResult.Success(List.of()),
                new CreateServiceUseCase.CreateServiceResult.Failure(),
                new DeleteServiceUseCase.DeleteServiceResult.Success()
        ));

        mockMvc.perform(post("/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Deployko Api",
                                  "imageRepository": "ghcr.io/deployko/api"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_internal_server_error_when_create_fails_unexpectedly() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceUseCases(
                new GetServiceSummariesUseCase.GetServiceSummariesResult.Success(List.of()),
                new CreateServiceUseCase.CreateServiceResult.Failure(),
                new DeleteServiceUseCase.DeleteServiceResult.Success()
        ));

        mockMvc.perform(post("/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "deployko-api",
                                  "imageRepository": "ghcr.io/deployko/api"
                                }
                                """))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void deletes_service_and_returns_no_content() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceUseCases(
                new GetServiceSummariesUseCase.GetServiceSummariesResult.Success(List.of()),
                new CreateServiceUseCase.CreateServiceResult.Failure(),
                new DeleteServiceUseCase.DeleteServiceResult.Success()
        ));

        mockMvc.perform(delete("/services/{serviceName}", "deployko-api"))
                .andExpect(status().isNoContent());
    }

    @Test
    void returns_not_found_when_deleting_missing_service() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceUseCases(
                new GetServiceSummariesUseCase.GetServiceSummariesResult.Success(List.of()),
                new CreateServiceUseCase.CreateServiceResult.Failure(),
                new DeleteServiceUseCase.DeleteServiceResult.NotFound()
        ));

        mockMvc.perform(delete("/services/{serviceName}", "deployko-api"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_bad_request_when_delete_service_name_is_invalid() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceUseCases(
                new GetServiceSummariesUseCase.GetServiceSummariesResult.Success(List.of()),
                new CreateServiceUseCase.CreateServiceResult.Failure(),
                new DeleteServiceUseCase.DeleteServiceResult.Success()
        ));

        mockMvc.perform(delete("/services/{serviceName}", "Deployko Api"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_internal_server_error_when_delete_fails_unexpectedly() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceUseCases(
                new GetServiceSummariesUseCase.GetServiceSummariesResult.Success(List.of()),
                new CreateServiceUseCase.CreateServiceResult.Failure(),
                new DeleteServiceUseCase.DeleteServiceResult.Failure()
        ));

        mockMvc.perform(delete("/services/{serviceName}", "deployko-api"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void returns_service_summaries() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceUseCases(
                new GetServiceSummariesUseCase.GetServiceSummariesResult.Success(List.of(
                        new ServiceSummary(
                                new ServiceName("billing-api"),
                                new ImageRepository("ghcr.io/deployko/billing-api"),
                                Optional.of(new ImageVersion("1.0.0")),
                                ServiceRuntimeStatus.RUNNING
                        ),
                        new ServiceSummary(
                                new ServiceName("deployko-api"),
                                new ImageRepository("ghcr.io/deployko/api"),
                                Optional.empty(),
                                ServiceRuntimeStatus.NOT_DEPLOYED
                        )
                )),
                new CreateServiceUseCase.CreateServiceResult.Failure(),
                new DeleteServiceUseCase.DeleteServiceResult.Success()
        ));

        mockMvc.perform(get("/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.services[0].name").value("billing-api"))
                .andExpect(jsonPath("$.services[0].imageRepository").value("ghcr.io/deployko/billing-api"))
                .andExpect(jsonPath("$.services[0].deployedVersion").value("1.0.0"))
                .andExpect(jsonPath("$.services[0].status").value("RUNNING"))
                .andExpect(jsonPath("$.services[1].name").value("deployko-api"))
                .andExpect(jsonPath("$.services[1].imageRepository").value("ghcr.io/deployko/api"))
                .andExpect(jsonPath("$.services[1].deployedVersion").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.services[1].status").value("NOT_DEPLOYED"));
    }

    @Test
    void returns_internal_server_error_when_get_service_summaries_fails_unexpectedly() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceUseCases(
                new GetServiceSummariesUseCase.GetServiceSummariesResult.Failure(),
                new CreateServiceUseCase.CreateServiceResult.Failure(),
                new DeleteServiceUseCase.DeleteServiceResult.Success()
        ));

        mockMvc.perform(get("/services"))
                .andExpect(status().isInternalServerError());
    }

    private static MockMvc mockMvc(StubServiceUseCases useCases) {
        return MockMvcBuilders.standaloneSetup(new ServiceController(useCases, useCases, useCases)).build();
    }

    private record StubServiceUseCases(
            GetServiceSummariesUseCase.GetServiceSummariesResult getServiceSummariesResult,
            CreateServiceUseCase.CreateServiceResult createServiceResult,
            DeleteServiceUseCase.DeleteServiceResult deleteServiceResult
    ) implements GetServiceSummariesUseCase, CreateServiceUseCase, DeleteServiceUseCase {

        @Override
        public GetServiceSummariesResult getServiceSummaries() {
            return getServiceSummariesResult;
        }

        @Override
        public CreateServiceResult createService(CreateServiceCommand command) {
            return createServiceResult;
        }

        @Override
        public DeleteServiceResult deleteService(DeleteServiceCommand command) {
            return deleteServiceResult;
        }
    }
}
