package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.PortMappings;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMounts;
import hr.tvz.popovic.deployko.application.port.in.CreateServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.CreateServiceVolumeMountUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServiceVolumeMountUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServicePortMappingsUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceVolumeMountsUseCase;
import hr.tvz.popovic.deployko.application.port.in.UpdateServiceVolumeMountUseCase;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.CreateServiceVolumeMountPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceVolumeMountPort;
import hr.tvz.popovic.deployko.application.port.out.FindServicePortMappingsPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceVolumeMountsPort;
import hr.tvz.popovic.deployko.application.port.out.UpdateServiceVolumeMountPort;
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
    private static final VolumeMount VOLUME_MOUNT = new VolumeMount.BindMount(
            new VolumeMount.HostPath("/opt/deployko/config"),
            new VolumeMount.Target("/app/config"),
            true
    );

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

    @Test
    void create_volume_mount_returns_success_when_mount_is_created() {
        ServiceRuntimeConfigurationDomainService service = serviceWithVolumeMountCreator(
                (_, _) -> new CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.Created()
        );

        CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult result = service.createServiceVolumeMount(
                createVolumeMountCommand()
        );

        assertThat(result)
                .isInstanceOf(CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.Success.class);
    }

    @Test
    void create_volume_mount_returns_service_not_found_when_service_does_not_exist() {
        ServiceRuntimeConfigurationDomainService service = serviceWithVolumeMountCreator(
                (_, _) -> new CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.ServiceNotFound()
        );

        CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult result = service.createServiceVolumeMount(
                createVolumeMountCommand()
        );

        assertThat(result)
                .isInstanceOf(CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.ServiceNotFound.class);
    }

    @Test
    void create_volume_mount_returns_already_exists_when_mount_conflicts() {
        ServiceRuntimeConfigurationDomainService service = serviceWithVolumeMountCreator(
                (_, _) -> new CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.AlreadyExists()
        );

        CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult result = service.createServiceVolumeMount(
                createVolumeMountCommand()
        );

        assertThat(result)
                .isInstanceOf(CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.AlreadyExists.class);
    }

    @Test
    void create_volume_mount_returns_failure_when_persistence_fails() {
        ServiceRuntimeConfigurationDomainService service = serviceWithVolumeMountCreator(
                (_, _) -> new CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.Failure()
        );

        CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult result = service.createServiceVolumeMount(
                createVolumeMountCommand()
        );

        assertThat(result)
                .isInstanceOf(CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.Failure.class);
    }

    @Test
    void update_volume_mount_returns_success_when_mount_is_updated() {
        ServiceRuntimeConfigurationDomainService service = serviceWithVolumeMountUpdater(
                (_, _) -> new UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.Updated()
        );

        UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult result = service.updateServiceVolumeMount(
                updateVolumeMountCommand()
        );

        assertThat(result)
                .isInstanceOf(UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.Success.class);
    }

    @Test
    void update_volume_mount_returns_service_not_found_when_service_does_not_exist() {
        ServiceRuntimeConfigurationDomainService service = serviceWithVolumeMountUpdater(
                (_, _) -> new UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.ServiceNotFound()
        );

        UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult result = service.updateServiceVolumeMount(
                updateVolumeMountCommand()
        );

        assertThat(result)
                .isInstanceOf(UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.ServiceNotFound.class);
    }

    @Test
    void update_volume_mount_returns_volume_mount_not_found_when_mount_does_not_exist() {
        ServiceRuntimeConfigurationDomainService service = serviceWithVolumeMountUpdater(
                (_, _) -> new UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.VolumeMountNotFound()
        );

        UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult result = service.updateServiceVolumeMount(
                updateVolumeMountCommand()
        );

        assertThat(result)
                .isInstanceOf(UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.VolumeMountNotFound.class);
    }

    @Test
    void update_volume_mount_returns_failure_when_persistence_fails() {
        ServiceRuntimeConfigurationDomainService service = serviceWithVolumeMountUpdater(
                (_, _) -> new UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.Failure()
        );

        UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult result = service.updateServiceVolumeMount(
                updateVolumeMountCommand()
        );

        assertThat(result)
                .isInstanceOf(UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.Failure.class);
    }

    @Test
    void delete_volume_mount_returns_success_when_mount_is_deleted() {
        ServiceRuntimeConfigurationDomainService service = serviceWithVolumeMountDeleter(
                (_, _) -> new DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.Deleted()
        );

        DeleteServiceVolumeMountUseCase.DeleteServiceVolumeMountResult result = service.deleteServiceVolumeMount(
                deleteVolumeMountCommand()
        );

        assertThat(result)
                .isInstanceOf(DeleteServiceVolumeMountUseCase.DeleteServiceVolumeMountResult.Success.class);
    }

    @Test
    void delete_volume_mount_returns_service_not_found_when_service_does_not_exist() {
        ServiceRuntimeConfigurationDomainService service = serviceWithVolumeMountDeleter(
                (_, _) -> new DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.ServiceNotFound()
        );

        DeleteServiceVolumeMountUseCase.DeleteServiceVolumeMountResult result = service.deleteServiceVolumeMount(
                deleteVolumeMountCommand()
        );

        assertThat(result)
                .isInstanceOf(DeleteServiceVolumeMountUseCase.DeleteServiceVolumeMountResult.ServiceNotFound.class);
    }

    @Test
    void delete_volume_mount_returns_volume_mount_not_found_when_mount_does_not_exist() {
        ServiceRuntimeConfigurationDomainService service = serviceWithVolumeMountDeleter(
                (_, _) -> new DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.VolumeMountNotFound()
        );

        DeleteServiceVolumeMountUseCase.DeleteServiceVolumeMountResult result = service.deleteServiceVolumeMount(
                deleteVolumeMountCommand()
        );

        assertThat(result)
                .isInstanceOf(DeleteServiceVolumeMountUseCase.DeleteServiceVolumeMountResult.VolumeMountNotFound.class);
    }

    @Test
    void delete_volume_mount_returns_failure_when_persistence_fails() {
        ServiceRuntimeConfigurationDomainService service = serviceWithVolumeMountDeleter(
                (_, _) -> new DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.Failure()
        );

        DeleteServiceVolumeMountUseCase.DeleteServiceVolumeMountResult result = service.deleteServiceVolumeMount(
                deleteVolumeMountCommand()
        );

        assertThat(result)
                .isInstanceOf(DeleteServiceVolumeMountUseCase.DeleteServiceVolumeMountResult.Failure.class);
    }

    private static ServiceRuntimeConfigurationDomainService serviceWithPortMappingFinder(
            FindServicePortMappingsPort findPort
    ) {
        return new ServiceRuntimeConfigurationDomainService(
                findPort,
                (_, _, _) -> new CreateServicePortMappingPort.CreateServicePortMappingResult.Failure(),
                (_, _) -> new DeleteServicePortMappingPort.DeleteServicePortMappingResult.Failure(),
                _ -> new FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.Failure(),
                (_, _) -> new CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.Failure(),
                (_, _) -> new UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.Failure(),
                (_, _) -> new DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.Failure()
        );
    }

    private static ServiceRuntimeConfigurationDomainService serviceWithPortMappingCreator(
            CreateServicePortMappingPort createPort
    ) {
        return new ServiceRuntimeConfigurationDomainService(
                _ -> new FindServicePortMappingsPort.FindServicePortMappingsResult.Failure(),
                createPort,
                (_, _) -> new DeleteServicePortMappingPort.DeleteServicePortMappingResult.Failure(),
                _ -> new FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.Failure(),
                (_, _) -> new CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.Failure(),
                (_, _) -> new UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.Failure(),
                (_, _) -> new DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.Failure()
        );
    }

    private static ServiceRuntimeConfigurationDomainService serviceWithPortMappingDeleter(
            DeleteServicePortMappingPort deletePort
    ) {
        return new ServiceRuntimeConfigurationDomainService(
                _ -> new FindServicePortMappingsPort.FindServicePortMappingsResult.Failure(),
                (_, _, _) -> new CreateServicePortMappingPort.CreateServicePortMappingResult.Failure(),
                deletePort,
                _ -> new FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.Failure(),
                (_, _) -> new CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.Failure(),
                (_, _) -> new UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.Failure(),
                (_, _) -> new DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.Failure()
        );
    }

    private static ServiceRuntimeConfigurationDomainService serviceWithVolumeMountFinder(
            FindServiceVolumeMountsPort findPort
    ) {
        return new ServiceRuntimeConfigurationDomainService(
                _ -> new FindServicePortMappingsPort.FindServicePortMappingsResult.Failure(),
                (_, _, _) -> new CreateServicePortMappingPort.CreateServicePortMappingResult.Failure(),
                (_, _) -> new DeleteServicePortMappingPort.DeleteServicePortMappingResult.Failure(),
                findPort,
                (_, _) -> new CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.Failure(),
                (_, _) -> new UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.Failure(),
                (_, _) -> new DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.Failure()
        );
    }

    private static ServiceRuntimeConfigurationDomainService serviceWithVolumeMountCreator(
            CreateServiceVolumeMountPort createPort
    ) {
        return new ServiceRuntimeConfigurationDomainService(
                _ -> new FindServicePortMappingsPort.FindServicePortMappingsResult.Failure(),
                (_, _, _) -> new CreateServicePortMappingPort.CreateServicePortMappingResult.Failure(),
                (_, _) -> new DeleteServicePortMappingPort.DeleteServicePortMappingResult.Failure(),
                _ -> new FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.Failure(),
                createPort,
                (_, _) -> new UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.Failure(),
                (_, _) -> new DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.Failure()
        );
    }

    private static ServiceRuntimeConfigurationDomainService serviceWithVolumeMountUpdater(
            UpdateServiceVolumeMountPort updatePort
    ) {
        return new ServiceRuntimeConfigurationDomainService(
                _ -> new FindServicePortMappingsPort.FindServicePortMappingsResult.Failure(),
                (_, _, _) -> new CreateServicePortMappingPort.CreateServicePortMappingResult.Failure(),
                (_, _) -> new DeleteServicePortMappingPort.DeleteServicePortMappingResult.Failure(),
                _ -> new FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.Failure(),
                (_, _) -> new CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.Failure(),
                updatePort,
                (_, _) -> new DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.Failure()
        );
    }

    private static ServiceRuntimeConfigurationDomainService serviceWithVolumeMountDeleter(
            DeleteServiceVolumeMountPort deletePort
    ) {
        return new ServiceRuntimeConfigurationDomainService(
                _ -> new FindServicePortMappingsPort.FindServicePortMappingsResult.Failure(),
                (_, _, _) -> new CreateServicePortMappingPort.CreateServicePortMappingResult.Failure(),
                (_, _) -> new DeleteServicePortMappingPort.DeleteServicePortMappingResult.Failure(),
                _ -> new FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.Failure(),
                (_, _) -> new CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.Failure(),
                (_, _) -> new UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.Failure(),
                deletePort
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

    private static CreateServiceVolumeMountUseCase.CreateServiceVolumeMountCommand createVolumeMountCommand() {
        return new CreateServiceVolumeMountUseCase.CreateServiceVolumeMountCommand(SERVICE_NAME, VOLUME_MOUNT);
    }

    private static UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountCommand updateVolumeMountCommand() {
        return new UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountCommand(SERVICE_NAME, VOLUME_MOUNT);
    }

    private static DeleteServiceVolumeMountUseCase.DeleteServiceVolumeMountCommand deleteVolumeMountCommand() {
        return new DeleteServiceVolumeMountUseCase.DeleteServiceVolumeMountCommand(SERVICE_NAME, VOLUME_MOUNT.target());
    }
}
