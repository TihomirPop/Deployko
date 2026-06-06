package hr.tvz.popovic.deployko.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.RuntimeConfiguration;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.CreateServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServiceUseCase;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceByNamePort;
import org.junit.jupiter.api.Test;

class ServiceDefinitionDomainServiceTest {

    @Test
    void creates_service_when_service_does_not_exist() {
        ServiceName serviceName = new ServiceName("deployko-api");
        ImageRepository imageRepository = new ImageRepository("ghcr.io/deployko/api");
        ServiceDefinitionDomainService service = new ServiceDefinitionDomainService(
                savedService -> new CreateServicePort.CreateServicePortResult.Success(),
                _ -> new DeleteServiceByNamePort.DeleteServiceByNameResult.Deleted()
        );

        CreateServiceUseCase.CreateServiceResult result = service.createService(
                new CreateServiceUseCase.CreateServiceCommand(serviceName, imageRepository)
        );

        assertThat(result).isInstanceOf(CreateServiceUseCase.CreateServiceResult.Success.class);
        Service createdService = ((CreateServiceUseCase.CreateServiceResult.Success) result).service();
        assertThat(createdService.name()).isEqualTo(serviceName);
        assertThat(createdService.imageRepository()).isEqualTo(imageRepository);
        assertThat(createdService.runtimeConfiguration()).isEqualTo(RuntimeConfiguration.empty());
    }

    @Test
    void returns_duplicate_service_name_when_service_already_exists() {
        ServiceDefinitionDomainService service = new ServiceDefinitionDomainService(
                _ -> new CreateServicePort.CreateServicePortResult.AlreadyExists(),
                _ -> new DeleteServiceByNamePort.DeleteServiceByNameResult.Deleted()
        );

        CreateServiceUseCase.CreateServiceResult result = service.createService(
                new CreateServiceUseCase.CreateServiceCommand(
                        new ServiceName("deployko-api"),
                        new ImageRepository("ghcr.io/deployko/api")
                )
        );

        assertThat(result).isInstanceOf(CreateServiceUseCase.CreateServiceResult.DuplicateServiceName.class);
    }

    @Test
    void returns_failure_when_create_service_port_fails_during_create() {
        ServiceDefinitionDomainService service = new ServiceDefinitionDomainService(
                _ -> new CreateServicePort.CreateServicePortResult.Failure(),
                _ -> new DeleteServiceByNamePort.DeleteServiceByNameResult.Deleted()
        );

        CreateServiceUseCase.CreateServiceResult result = service.createService(
                new CreateServiceUseCase.CreateServiceCommand(
                        new ServiceName("deployko-api"),
                        new ImageRepository("ghcr.io/deployko/api")
                )
        );

        assertThat(result).isInstanceOf(CreateServiceUseCase.CreateServiceResult.Failure.class);
    }

    @Test
    void deletes_service_when_delete_port_reports_deleted() {
        ServiceDefinitionDomainService service = new ServiceDefinitionDomainService(
                _ -> new CreateServicePort.CreateServicePortResult.Success(),
                _ -> new DeleteServiceByNamePort.DeleteServiceByNameResult.Deleted()
        );

        DeleteServiceUseCase.DeleteServiceResult result = service.deleteService(
                new DeleteServiceUseCase.DeleteServiceCommand(new ServiceName("deployko-api"))
        );

        assertThat(result).isInstanceOf(DeleteServiceUseCase.DeleteServiceResult.Success.class);
    }

    @Test
    void returns_not_found_when_delete_port_reports_missing_service() {
        ServiceDefinitionDomainService service = new ServiceDefinitionDomainService(
                _ -> new CreateServicePort.CreateServicePortResult.Success(),
                _ -> new DeleteServiceByNamePort.DeleteServiceByNameResult.NotFound()
        );

        DeleteServiceUseCase.DeleteServiceResult result = service.deleteService(
                new DeleteServiceUseCase.DeleteServiceCommand(new ServiceName("deployko-api"))
        );

        assertThat(result).isInstanceOf(DeleteServiceUseCase.DeleteServiceResult.NotFound.class);
    }

    @Test
    void returns_deployment_exists_when_delete_port_reports_existing_deployment() {
        ServiceDefinitionDomainService service = new ServiceDefinitionDomainService(
                _ -> new CreateServicePort.CreateServicePortResult.Success(),
                _ -> new DeleteServiceByNamePort.DeleteServiceByNameResult.DeploymentExists()
        );

        DeleteServiceUseCase.DeleteServiceResult result = service.deleteService(
                new DeleteServiceUseCase.DeleteServiceCommand(new ServiceName("deployko-api"))
        );

        assertThat(result).isInstanceOf(DeleteServiceUseCase.DeleteServiceResult.DeploymentExists.class);
    }

    @Test
    void returns_failure_when_delete_port_fails() {
        ServiceDefinitionDomainService service = new ServiceDefinitionDomainService(
                _ -> new CreateServicePort.CreateServicePortResult.Success(),
                _ -> new DeleteServiceByNamePort.DeleteServiceByNameResult.Failure()
        );

        DeleteServiceUseCase.DeleteServiceResult result = service.deleteService(
                new DeleteServiceUseCase.DeleteServiceCommand(new ServiceName("deployko-api"))
        );

        assertThat(result).isInstanceOf(DeleteServiceUseCase.DeleteServiceResult.Failure.class);
    }

}
