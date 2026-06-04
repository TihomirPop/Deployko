package hr.tvz.popovic.deployko.adapter.in;

import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.GetServiceVersionsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServiceVersionControllerTest {

    @Test
    void returns_service_versions() throws Exception {
        StubGetServiceVersionsUseCase useCase = new StubGetServiceVersionsUseCase(
                new GetServiceVersionsUseCase.GetServiceVersionsResult.Success(List.of(
                        new ImageVersion("1.0.0"),
                        new ImageVersion("latest")
                ))
        );
        MockMvc mockMvc = mockMvc(useCase);

        mockMvc.perform(get("/services/{serviceName}/versions", "deployko-api"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "imageVersions": ["1.0.0", "latest"]
                        }
                        """));

        assertThat(useCase.command).isEqualTo(new GetServiceVersionsUseCase.GetServiceVersionsCommand(
                new ServiceName("deployko-api")
        ));
    }

    @Test
    void returns_not_found_when_service_is_missing() throws Exception {
        MockMvc mockMvc = mockMvc(new StubGetServiceVersionsUseCase(
                new GetServiceVersionsUseCase.GetServiceVersionsResult.NotFound()
        ));

        mockMvc.perform(get("/services/{serviceName}/versions", "missing-service"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_bad_request_when_service_name_is_invalid() throws Exception {
        MockMvc mockMvc = mockMvc(new StubGetServiceVersionsUseCase(
                new GetServiceVersionsUseCase.GetServiceVersionsResult.Success(List.of(new ImageVersion("1.0.0")))
        ));

        mockMvc.perform(get("/services/{serviceName}/versions", "Deployko Api"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_internal_server_error_when_lookup_fails() throws Exception {
        MockMvc mockMvc = mockMvc(new StubGetServiceVersionsUseCase(
                new GetServiceVersionsUseCase.GetServiceVersionsResult.Failure()
        ));

        mockMvc.perform(get("/services/{serviceName}/versions", "deployko-api"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void returns_empty_list_when_service_has_no_versions() throws Exception {
        MockMvc mockMvc = mockMvc(new StubGetServiceVersionsUseCase(
                new GetServiceVersionsUseCase.GetServiceVersionsResult.Success(List.of())
        ));

        mockMvc.perform(get("/services/{serviceName}/versions", "deployko-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageVersions").isEmpty());
    }

    private static MockMvc mockMvc(GetServiceVersionsUseCase getServiceVersionsUseCase) {
        return MockMvcBuilders
                .standaloneSetup(new ServiceVersionController(getServiceVersionsUseCase))
                .build();
    }

    private static final class StubGetServiceVersionsUseCase implements GetServiceVersionsUseCase {

        private final GetServiceVersionsResult result;
        private GetServiceVersionsCommand command;

        private StubGetServiceVersionsUseCase(GetServiceVersionsResult result) {
            this.result = result;
        }

        @Override
        public GetServiceVersionsResult getServiceVersions(GetServiceVersionsCommand command) {
            this.command = command;
            return result;
        }
    }
}
