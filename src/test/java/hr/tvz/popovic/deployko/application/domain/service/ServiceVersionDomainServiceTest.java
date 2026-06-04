package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.RuntimeConfiguration;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.GetServiceVersionsUseCase;
import hr.tvz.popovic.deployko.application.port.out.FindImageVersionsPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceDefinitionPort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceVersionDomainServiceTest {

    private static final Service SERVICE = new Service(
            new ServiceName("deployko-api"),
            new ImageRepository("ghcr.io/deployko/api"),
            RuntimeConfiguration.empty()
    );

    @Test
    void returns_versions_for_existing_service() {
        FakeFindImageVersionsPort findImageVersionsPort = new FakeFindImageVersionsPort(
                new FindImageVersionsPort.FindImageVersionsResult.Found(List.of(
                        new ImageVersion("1.0.0"),
                        new ImageVersion("latest")
                ))
        );
        ServiceVersionDomainService service = new ServiceVersionDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.Found(SERVICE),
                findImageVersionsPort
        );

        GetServiceVersionsUseCase.GetServiceVersionsResult result = service.getServiceVersions(
                new GetServiceVersionsUseCase.GetServiceVersionsCommand(SERVICE.name())
        );

        assertThat(result).isEqualTo(new GetServiceVersionsUseCase.GetServiceVersionsResult.Success(List.of(
                new ImageVersion("1.0.0"),
                new ImageVersion("latest")
        )));
        assertThat(findImageVersionsPort.imageRepository).isEqualTo(SERVICE.imageRepository());
    }

    @Test
    void returns_not_found_when_service_definition_is_missing() {
        ServiceVersionDomainService service = new ServiceVersionDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.NotFound(),
                _ -> new FindImageVersionsPort.FindImageVersionsResult.Found(List.of(new ImageVersion("1.0.0")))
        );

        GetServiceVersionsUseCase.GetServiceVersionsResult result = service.getServiceVersions(
                new GetServiceVersionsUseCase.GetServiceVersionsCommand(SERVICE.name())
        );

        assertThat(result).isInstanceOf(GetServiceVersionsUseCase.GetServiceVersionsResult.NotFound.class);
    }

    @Test
    void returns_failure_when_service_definition_lookup_fails() {
        ServiceVersionDomainService service = new ServiceVersionDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.Failure(),
                _ -> new FindImageVersionsPort.FindImageVersionsResult.Found(List.of(new ImageVersion("1.0.0")))
        );

        GetServiceVersionsUseCase.GetServiceVersionsResult result = service.getServiceVersions(
                new GetServiceVersionsUseCase.GetServiceVersionsCommand(SERVICE.name())
        );

        assertThat(result).isInstanceOf(GetServiceVersionsUseCase.GetServiceVersionsResult.Failure.class);
    }

    @Test
    void returns_failure_when_registry_lookup_fails() {
        ServiceVersionDomainService service = new ServiceVersionDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.Found(SERVICE),
                _ -> new FindImageVersionsPort.FindImageVersionsResult.Failure()
        );

        GetServiceVersionsUseCase.GetServiceVersionsResult result = service.getServiceVersions(
                new GetServiceVersionsUseCase.GetServiceVersionsCommand(SERVICE.name())
        );

        assertThat(result).isInstanceOf(GetServiceVersionsUseCase.GetServiceVersionsResult.Failure.class);
    }

    private static final class FakeFindImageVersionsPort implements FindImageVersionsPort {

        private final FindImageVersionsResult result;
        private ImageRepository imageRepository;

        private FakeFindImageVersionsPort(FindImageVersionsResult result) {
            this.result = result;
        }

        @Override
        public FindImageVersionsResult findImageVersions(ImageRepository imageRepository) {
            this.imageRepository = imageRepository;
            return result;
        }
    }
}
