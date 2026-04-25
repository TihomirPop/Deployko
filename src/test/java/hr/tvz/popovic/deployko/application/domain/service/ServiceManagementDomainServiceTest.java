package hr.tvz.popovic.deployko.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.RuntimeConfiguration;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.ServiceManagementUseCase;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceByNamePort;
import org.junit.jupiter.api.Test;

class ServiceManagementDomainServiceTest {

    @Test
    void creates_service_when_service_does_not_exist() {
        ServiceName serviceName = new ServiceName("deployko-api");
        ImageRepository imageRepository = new ImageRepository("ghcr.io/deployko/api");
        ServiceManagementDomainService service = new ServiceManagementDomainService(
                savedService -> new CreateServicePort.CreateServicePortResult.Success(),
                _ -> new DeleteServiceByNamePort.DeleteServiceByNameResult.Deleted()
        );

        ServiceManagementUseCase.CreateServiceResult result = service.createService(
                new ServiceManagementUseCase.CreateServiceCommand(serviceName, imageRepository)
        );

        assertThat(result).isInstanceOf(ServiceManagementUseCase.CreateServiceResult.Success.class);
        Service createdService = ((ServiceManagementUseCase.CreateServiceResult.Success) result).service();
        assertThat(createdService.name()).isEqualTo(serviceName);
        assertThat(createdService.imageRepository()).isEqualTo(imageRepository);
        assertThat(createdService.runtimeConfiguration()).isEqualTo(RuntimeConfiguration.empty());
    }

    @Test
    void returns_duplicate_service_name_when_service_already_exists() {
        ServiceManagementDomainService service = new ServiceManagementDomainService(
                _ -> new CreateServicePort.CreateServicePortResult.AlreadyExists(),
                _ -> new DeleteServiceByNamePort.DeleteServiceByNameResult.Deleted()
        );

        ServiceManagementUseCase.CreateServiceResult result = service.createService(
                new ServiceManagementUseCase.CreateServiceCommand(
                        new ServiceName("deployko-api"),
                        new ImageRepository("ghcr.io/deployko/api")
                )
        );

        assertThat(result).isInstanceOf(ServiceManagementUseCase.CreateServiceResult.DuplicateServiceName.class);
    }

    @Test
    void returns_failure_when_create_service_port_fails_during_create() {
        ServiceManagementDomainService service = new ServiceManagementDomainService(
                _ -> new CreateServicePort.CreateServicePortResult.Failure(),
                _ -> new DeleteServiceByNamePort.DeleteServiceByNameResult.Deleted()
        );

        ServiceManagementUseCase.CreateServiceResult result = service.createService(
                new ServiceManagementUseCase.CreateServiceCommand(
                        new ServiceName("deployko-api"),
                        new ImageRepository("ghcr.io/deployko/api")
                )
        );

        assertThat(result).isInstanceOf(ServiceManagementUseCase.CreateServiceResult.Failure.class);
    }

    @Test
    void deletes_service_when_delete_port_reports_deleted() {
        ServiceManagementDomainService service = new ServiceManagementDomainService(
                _ -> new CreateServicePort.CreateServicePortResult.Success(),
                _ -> new DeleteServiceByNamePort.DeleteServiceByNameResult.Deleted()
        );

        ServiceManagementUseCase.DeleteServiceResult result = service.deleteService(
                new ServiceManagementUseCase.DeleteServiceCommand(new ServiceName("deployko-api"))
        );

        assertThat(result).isInstanceOf(ServiceManagementUseCase.DeleteServiceResult.Success.class);
    }

    @Test
    void returns_not_found_when_delete_port_reports_missing_service() {
        ServiceManagementDomainService service = new ServiceManagementDomainService(
                _ -> new CreateServicePort.CreateServicePortResult.Success(),
                _ -> new DeleteServiceByNamePort.DeleteServiceByNameResult.NotFound()
        );

        ServiceManagementUseCase.DeleteServiceResult result = service.deleteService(
                new ServiceManagementUseCase.DeleteServiceCommand(new ServiceName("deployko-api"))
        );

        assertThat(result).isInstanceOf(ServiceManagementUseCase.DeleteServiceResult.NotFound.class);
    }

    @Test
    void returns_failure_when_delete_port_fails() {
        ServiceManagementDomainService service = new ServiceManagementDomainService(
                _ -> new CreateServicePort.CreateServicePortResult.Success(),
                _ -> new DeleteServiceByNamePort.DeleteServiceByNameResult.Failure()
        );

        ServiceManagementUseCase.DeleteServiceResult result = service.deleteService(
                new ServiceManagementUseCase.DeleteServiceCommand(new ServiceName("deployko-api"))
        );

        assertThat(result).isInstanceOf(ServiceManagementUseCase.DeleteServiceResult.Failure.class);
    }
}
