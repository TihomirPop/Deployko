package hr.tvz.popovic.deployko.adapter.in;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.RuntimeConfiguration;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.ServiceManagementUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ServiceManagementControllerTest {

    @Test
    void creates_service_and_returns_created_status() throws Exception {
        Service service = new Service(
                new ServiceName("deployko-api"),
                new ImageRepository("ghcr.io/deployko/api"),
                RuntimeConfiguration.empty()
        );
        MockMvc mockMvc = mockMvc(new StubServiceManagementUseCase(
                new ServiceManagementUseCase.CreateServiceResult.Success(service),
                new ServiceManagementUseCase.DeleteServiceResult.Success()
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
        MockMvc mockMvc = mockMvc(new StubServiceManagementUseCase(
                new ServiceManagementUseCase.CreateServiceResult.DuplicateServiceName(),
                new ServiceManagementUseCase.DeleteServiceResult.Success()
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
        MockMvc mockMvc = mockMvc(new StubServiceManagementUseCase(
                new ServiceManagementUseCase.CreateServiceResult.Failure(),
                new ServiceManagementUseCase.DeleteServiceResult.Success()
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
        MockMvc mockMvc = mockMvc(new StubServiceManagementUseCase(
                new ServiceManagementUseCase.CreateServiceResult.Failure(),
                new ServiceManagementUseCase.DeleteServiceResult.Success()
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
        MockMvc mockMvc = mockMvc(new StubServiceManagementUseCase(
                new ServiceManagementUseCase.CreateServiceResult.Failure(),
                new ServiceManagementUseCase.DeleteServiceResult.Success()
        ));

        mockMvc.perform(delete("/services/{serviceName}", "deployko-api"))
                .andExpect(status().isNoContent());
    }

    @Test
    void returns_not_found_when_deleting_missing_service() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceManagementUseCase(
                new ServiceManagementUseCase.CreateServiceResult.Failure(),
                new ServiceManagementUseCase.DeleteServiceResult.NotFound()
        ));

        mockMvc.perform(delete("/services/{serviceName}", "deployko-api"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_bad_request_when_delete_service_name_is_invalid() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceManagementUseCase(
                new ServiceManagementUseCase.CreateServiceResult.Failure(),
                new ServiceManagementUseCase.DeleteServiceResult.Success()
        ));

        mockMvc.perform(delete("/services/{serviceName}", "Deployko Api"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_internal_server_error_when_delete_fails_unexpectedly() throws Exception {
        MockMvc mockMvc = mockMvc(new StubServiceManagementUseCase(
                new ServiceManagementUseCase.CreateServiceResult.Failure(),
                new ServiceManagementUseCase.DeleteServiceResult.Failure()
        ));

        mockMvc.perform(delete("/services/{serviceName}", "deployko-api"))
                .andExpect(status().isInternalServerError());
    }

    private static MockMvc mockMvc(ServiceManagementUseCase serviceManagementUseCase) {
        return MockMvcBuilders.standaloneSetup(new ServiceManagementController(serviceManagementUseCase)).build();
    }

    private record StubServiceManagementUseCase(
            ServiceManagementUseCase.CreateServiceResult createServiceResult,
            ServiceManagementUseCase.DeleteServiceResult deleteServiceResult
    ) implements ServiceManagementUseCase {

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
