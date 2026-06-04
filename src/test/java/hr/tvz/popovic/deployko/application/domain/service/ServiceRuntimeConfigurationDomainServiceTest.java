package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.PortMappings;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMounts;
import hr.tvz.popovic.deployko.application.port.in.CreateServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServicePortMappingsUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceVolumeMountsUseCase;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.FindServicePortMappingsPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceVolumeMountsPort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceRuntimeConfigurationDomainServiceTest {

    private static final ServiceName SERVICE_NAME = new ServiceName("deployko-api");
    private static final PortMappings PORT_MAPPINGS = PortMappings.empty()
            .add(new Port(8080), new Port(80));
    private static final VolumeMounts VOLUME_MOUNTS = VolumeMounts.empty()
            .add(new VolumeMount.BindMount(
                    new VolumeMount.HostPath("/opt/deployko/config"),
                    new VolumeMount.Target("/app/config"),
                    true
            ));

    @Test
    void get_port_mappings_returns_success_when_service_exists() {
        ServiceRuntimeConfigurationDomainService service = serviceWithPortMappingFinder(
                _ -> new FindServicePortMappingsPort.FindServicePortMappingsResult.Found(PORT_MAPPINGS)
        );

        GetServicePortMappingsUseCase.GetServicePortMappingsResult result = service.getServicePortMappings(
                new GetServicePortMappingsUseCase.GetServicePortMappingsCommand(SERVICE_NAME)
        );

        assertThat(result)
                .isInstanceOf(GetServicePortMappingsUseCase.GetServicePortMappingsResult.Success.class);
        GetServicePortMappingsUseCase.GetServicePortMappingsResult.Success success =
                (GetServicePortMappingsUseCase.GetServicePortMappingsResult.Success) result;
        assertThat(success.portMappings()).isEqualTo(PORT_MAPPINGS);
    }

    @Test
    void get_port_mappings_returns_not_found_when_service_does_not_exist() {
        ServiceRuntimeConfigurationDomainService service = serviceWithPortMappingFinder(
                _ -> new FindServicePortMappingsPort.FindServicePortMappingsResult.ServiceNotFound()
        );

        GetServicePortMappingsUseCase.GetServicePortMappingsResult result = service.getServicePortMappings(
                new GetServicePortMappingsUseCase.GetServicePortMappingsCommand(SERVICE_NAME)
        );

        assertThat(result)
                .isInstanceOf(GetServicePortMappingsUseCase.GetServicePortMappingsResult.NotFound.class);
    }

    @Test
    void get_port_mappings_returns_failure_when_persistence_fails() {
        ServiceRuntimeConfigurationDomainService service = serviceWithPortMappingFinder(
                _ -> new FindServicePortMappingsPort.FindServicePortMappingsResult.Failure()
        );

        GetServicePortMappingsUseCase.GetServicePortMappingsResult result = service.getServicePortMappings(
                new GetServicePortMappingsUseCase.GetServicePortMappingsCommand(SERVICE_NAME)
        );

        assertThat(result)
                .isInstanceOf(GetServicePortMappingsUseCase.GetServicePortMappingsResult.Failure.class);
    }

    @Test
    void create_port_mapping_returns_success_when_mapping_is_created() {
        ServiceRuntimeConfigurationDomainService service = serviceWithPortMappingCreator(
                (_, _, _) -> new CreateServicePortMappingPort.CreateServicePortMappingResult.Created()
        );

        CreateServicePortMappingUseCase.CreateServicePortMappingResult result = service.createServicePortMapping(
                createCommand()
        );

        assertThat(result)
                .isInstanceOf(CreateServicePortMappingUseCase.CreateServicePortMappingResult.Success.class);
    }

    @Test
    void create_port_mapping_returns_service_not_found_when_service_does_not_exist() {
        ServiceRuntimeConfigurationDomainService service = serviceWithPortMappingCreator(
                (_, _, _) -> new CreateServicePortMappingPort.CreateServicePortMappingResult.ServiceNotFound()
        );

        CreateServicePortMappingUseCase.CreateServicePortMappingResult result = service.createServicePortMapping(
                createCommand()
        );

        assertThat(result)
                .isInstanceOf(CreateServicePortMappingUseCase.CreateServicePortMappingResult.ServiceNotFound.class);
    }

    @Test
    void create_port_mapping_returns_already_exists_when_mapping_conflicts() {
        ServiceRuntimeConfigurationDomainService service = serviceWithPortMappingCreator(
                (_, _, _) -> new CreateServicePortMappingPort.CreateServicePortMappingResult.AlreadyExists()
        );

        CreateServicePortMappingUseCase.CreateServicePortMappingResult result = service.createServicePortMapping(
                createCommand()
        );

        assertThat(result)
                .isInstanceOf(CreateServicePortMappingUseCase.CreateServicePortMappingResult.AlreadyExists.class);
    }

    @Test
    void create_port_mapping_returns_failure_when_persistence_fails() {
        ServiceRuntimeConfigurationDomainService service = serviceWithPortMappingCreator(
                (_, _, _) -> new CreateServicePortMappingPort.CreateServicePortMappingResult.Failure()
        );

        CreateServicePortMappingUseCase.CreateServicePortMappingResult result = service.createServicePortMapping(
                createCommand()
        );

        assertThat(result)
                .isInstanceOf(CreateServicePortMappingUseCase.CreateServicePortMappingResult.Failure.class);
    }

    @Test
    void delete_port_mapping_returns_success_when_mapping_is_deleted() {
        ServiceRuntimeConfigurationDomainService service = serviceWithPortMappingDeleter(
                (_, _) -> new DeleteServicePortMappingPort.DeleteServicePortMappingResult.Deleted()
        );

        DeleteServicePortMappingUseCase.DeleteServicePortMappingResult result = service.deleteServicePortMapping(
                deleteCommand()
        );

        assertThat(result)
                .isInstanceOf(DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.Success.class);
    }

    @Test
    void delete_port_mapping_returns_service_not_found_when_service_does_not_exist() {
        ServiceRuntimeConfigurationDomainService service = serviceWithPortMappingDeleter(
                (_, _) -> new DeleteServicePortMappingPort.DeleteServicePortMappingResult.ServiceNotFound()
        );

        DeleteServicePortMappingUseCase.DeleteServicePortMappingResult result = service.deleteServicePortMapping(
                deleteCommand()
        );

        assertThat(result)
                .isInstanceOf(DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.ServiceNotFound.class);
    }

    @Test
    void delete_port_mapping_returns_port_mapping_not_found_when_mapping_does_not_exist() {
        ServiceRuntimeConfigurationDomainService service = serviceWithPortMappingDeleter(
                (_, _) -> new DeleteServicePortMappingPort.DeleteServicePortMappingResult.PortMappingNotFound()
        );

        DeleteServicePortMappingUseCase.DeleteServicePortMappingResult result = service.deleteServicePortMapping(
                deleteCommand()
        );

        assertThat(result)
                .isInstanceOf(DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.PortMappingNotFound.class);
    }

    @Test
    void delete_port_mapping_returns_failure_when_persistence_fails() {
        ServiceRuntimeConfigurationDomainService service = serviceWithPortMappingDeleter(
                (_, _) -> new DeleteServicePortMappingPort.DeleteServicePortMappingResult.Failure()
        );

        DeleteServicePortMappingUseCase.DeleteServicePortMappingResult result = service.deleteServicePortMapping(
                deleteCommand()
        );

        assertThat(result)
                .isInstanceOf(DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.Failure.class);
    }

    @Test
    void get_volume_mounts_returns_success_when_service_exists() {
        ServiceRuntimeConfigurationDomainService service = serviceWithVolumeMountFinder(
                _ -> new FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.Found(VOLUME_MOUNTS)
        );

        GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult result = service.getServiceVolumeMounts(
                new GetServiceVolumeMountsUseCase.GetServiceVolumeMountsCommand(SERVICE_NAME)
        );

        assertThat(result)
                .isInstanceOf(GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult.Success.class);
        GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult.Success success =
                (GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult.Success) result;
        assertThat(success.volumeMounts()).isEqualTo(VOLUME_MOUNTS);
    }

    @Test
    void get_volume_mounts_returns_not_found_when_service_does_not_exist() {
        ServiceRuntimeConfigurationDomainService service = serviceWithVolumeMountFinder(
                _ -> new FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.ServiceNotFound()
        );

        GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult result = service.getServiceVolumeMounts(
                new GetServiceVolumeMountsUseCase.GetServiceVolumeMountsCommand(SERVICE_NAME)
        );

        assertThat(result)
                .isInstanceOf(GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult.NotFound.class);
    }

    @Test
    void get_volume_mounts_returns_failure_when_persistence_fails() {
        ServiceRuntimeConfigurationDomainService service = serviceWithVolumeMountFinder(
                _ -> new FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.Failure()
        );

        GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult result = service.getServiceVolumeMounts(
                new GetServiceVolumeMountsUseCase.GetServiceVolumeMountsCommand(SERVICE_NAME)
        );

        assertThat(result)
                .isInstanceOf(GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult.Failure.class);
    }

    private static ServiceRuntimeConfigurationDomainService serviceWithPortMappingFinder(
            FindServicePortMappingsPort findPort
    ) {
        return new ServiceRuntimeConfigurationDomainService(
                findPort,
                (_, _, _) -> new CreateServicePortMappingPort.CreateServicePortMappingResult.Failure(),
                (_, _) -> new DeleteServicePortMappingPort.DeleteServicePortMappingResult.Failure(),
                _ -> new FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.Failure()
        );
    }

    private static ServiceRuntimeConfigurationDomainService serviceWithPortMappingCreator(
            CreateServicePortMappingPort createPort
    ) {
        return new ServiceRuntimeConfigurationDomainService(
                _ -> new FindServicePortMappingsPort.FindServicePortMappingsResult.Failure(),
                createPort,
                (_, _) -> new DeleteServicePortMappingPort.DeleteServicePortMappingResult.Failure(),
                _ -> new FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.Failure()
        );
    }

    private static ServiceRuntimeConfigurationDomainService serviceWithPortMappingDeleter(
            DeleteServicePortMappingPort deletePort
    ) {
        return new ServiceRuntimeConfigurationDomainService(
                _ -> new FindServicePortMappingsPort.FindServicePortMappingsResult.Failure(),
                (_, _, _) -> new CreateServicePortMappingPort.CreateServicePortMappingResult.Failure(),
                deletePort,
                _ -> new FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.Failure()
        );
    }

    private static ServiceRuntimeConfigurationDomainService serviceWithVolumeMountFinder(
            FindServiceVolumeMountsPort findPort
    ) {
        return new ServiceRuntimeConfigurationDomainService(
                _ -> new FindServicePortMappingsPort.FindServicePortMappingsResult.Failure(),
                (_, _, _) -> new CreateServicePortMappingPort.CreateServicePortMappingResult.Failure(),
                (_, _) -> new DeleteServicePortMappingPort.DeleteServicePortMappingResult.Failure(),
                findPort
        );
    }

    private static CreateServicePortMappingUseCase.CreateServicePortMappingCommand createCommand() {
        return new CreateServicePortMappingUseCase.CreateServicePortMappingCommand(
                SERVICE_NAME,
                new Port(8080),
                new Port(80)
        );
    }

    private static DeleteServicePortMappingUseCase.DeleteServicePortMappingCommand deleteCommand() {
        return new DeleteServicePortMappingUseCase.DeleteServicePortMappingCommand(
                SERVICE_NAME,
                new Port(8080)
        );
    }
}
