package hr.tvz.popovic.deployko.adapter.in;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.RuntimeConfiguration;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.CreateServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceNamesUseCase;
import java.util.List;
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
                new GetServiceNamesUseCase.GetServiceNamesResult.Success(List.of()),
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
                new GetServiceNamesUseCase.GetServiceNamesResult.Success(List.of()),
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
                new GetServiceNamesUseCase.GetServiceNamesResult.Success(List.of()),
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
                new GetServiceNamesUseCase.GetServiceNamesResult.Success(List.of()),
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
                new GetServiceNamesUseCase.GetServiceNamesResult.Success(List.of()),
                new CreateServiceUseCase.CreateServiceResult.Failure(),
                new DeleteServiceUseCase.DeleteServiceResult.Success()
        ));

        mockMvc.perform(delete("/services/{serviceName}", "deployko-api"))
                .andExpect(status().isNoContent());
    }

    @Test
    void returns_not_found_when_deleting_missing_service() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceUseCases(
                new GetServiceNamesUseCase.GetServiceNamesResult.Success(List.of()),
                new CreateServiceUseCase.CreateServiceResult.Failure(),
                new DeleteServiceUseCase.DeleteServiceResult.NotFound()
        ));

        mockMvc.perform(delete("/services/{serviceName}", "deployko-api"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_bad_request_when_delete_service_name_is_invalid() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceUseCases(
                new GetServiceNamesUseCase.GetServiceNamesResult.Success(List.of()),
                new CreateServiceUseCase.CreateServiceResult.Failure(),
                new DeleteServiceUseCase.DeleteServiceResult.Success()
        ));

        mockMvc.perform(delete("/services/{serviceName}", "Deployko Api"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_internal_server_error_when_delete_fails_unexpectedly() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceUseCases(
                new GetServiceNamesUseCase.GetServiceNamesResult.Success(List.of()),
                new CreateServiceUseCase.CreateServiceResult.Failure(),
                new DeleteServiceUseCase.DeleteServiceResult.Failure()
        ));

        mockMvc.perform(delete("/services/{serviceName}", "deployko-api"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void returns_service_names() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceUseCases(
                new GetServiceNamesUseCase.GetServiceNamesResult.Success(List.of(
                        new ServiceName("billing-api"),
                        new ServiceName("deployko-api")
                )),
                new CreateServiceUseCase.CreateServiceResult.Failure(),
                new DeleteServiceUseCase.DeleteServiceResult.Success()
        ));

        mockMvc.perform(get("/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceNames[0]").value("billing-api"))
                .andExpect(jsonPath("$.serviceNames[1]").value("deployko-api"));
    }

    @Test
    void returns_internal_server_error_when_get_service_names_fails_unexpectedly() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceUseCases(
                new GetServiceNamesUseCase.GetServiceNamesResult.Failure(),
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
            GetServiceNamesUseCase.GetServiceNamesResult getServiceNamesResult,
            CreateServiceUseCase.CreateServiceResult createServiceResult,
            DeleteServiceUseCase.DeleteServiceResult deleteServiceResult
    ) implements GetServiceNamesUseCase, CreateServiceUseCase, DeleteServiceUseCase {

        @Override
        public GetServiceNamesResult getServiceNames() {
            return getServiceNamesResult;
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
