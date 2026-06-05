package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.ActualDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachment;
import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachments;
import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.PortMappings;
import hr.tvz.popovic.deployko.application.domain.model.RuntimeConfiguration;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.ServiceRuntimeStatus;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMounts;
import hr.tvz.popovic.deployko.application.port.in.DeployServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceRuntimeStatusUseCase;
import hr.tvz.popovic.deployko.application.port.in.StartServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.StopServiceUseCase;
import hr.tvz.popovic.deployko.application.port.out.DeployContainerPort;
import hr.tvz.popovic.deployko.application.port.out.FindActualDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.FindDesiredDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceDefinitionPort;
import hr.tvz.popovic.deployko.application.port.out.StartContainerPort;
import hr.tvz.popovic.deployko.application.port.out.StopContainerPort;
import hr.tvz.popovic.deployko.application.port.out.UpdateDesiredDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.UpsertDesiredDeploymentPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceRuntimeDomainServiceTest {

    private static final Service SERVICE = service();
    private static final ImageVersion IMAGE_VERSION = new ImageVersion("1.0.0");

    @Test
    void deploys_service_when_definition_exists_and_ports_succeed() {
        FakeUpsertDesiredDeploymentPort upsertDesiredDeploymentPort = new FakeUpsertDesiredDeploymentPort(
                new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success()
        );
        FakeDeployContainerPort deployContainerPort = new FakeDeployContainerPort(
                new DeployContainerPort.DeployContainerResult.Success()
        );
        ServiceRuntimeDomainService service = new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.Found(SERVICE),
                upsertDesiredDeploymentPort,
                successfulUpdateStatePort(),
                successfulFindDesiredDeploymentStatePort(),
                deployContainerPort,
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                successfulFindActualDeploymentStatePort()
        );

        DeployServiceUseCase.DeployServiceResult result = service.deployService(
                new DeployServiceUseCase.DeployServiceCommand(SERVICE.name(), IMAGE_VERSION)
        );

        assertThat(result).isInstanceOf(DeployServiceUseCase.DeployServiceResult.Success.class);
        assertThat(upsertDesiredDeploymentPort.upsertedDeployments).hasSize(1);
        assertThat(upsertDesiredDeploymentPort.upsertedDeployments.getFirst()).isEqualTo(new DesiredDeployment(
                SERVICE.name(),
                SERVICE.imageRepository(),
                IMAGE_VERSION,
                SERVICE.runtimeConfiguration(),
                DesiredDeploymentState.RUNNING
        ));
        assertThat(deployContainerPort.deployedDeployments)
                .containsExactly(upsertDesiredDeploymentPort.upsertedDeployments.getFirst());
    }

    @Test
    void returns_service_not_found_when_service_definition_is_missing() {
        ServiceRuntimeDomainService service = new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.NotFound(),
                _ -> new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success(),
                successfulUpdateStatePort(),
                successfulFindDesiredDeploymentStatePort(),
                _ -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                successfulFindActualDeploymentStatePort()
        );

        DeployServiceUseCase.DeployServiceResult result = service.deployService(
                new DeployServiceUseCase.DeployServiceCommand(SERVICE.name(), IMAGE_VERSION)
        );

        assertThat(result).isInstanceOf(DeployServiceUseCase.DeployServiceResult.ServiceNotFound.class);
    }

    @Test
    void returns_desired_state_failure_when_service_definition_lookup_fails() {
        ServiceRuntimeDomainService service = new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.Failure(),
                _ -> new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success(),
                successfulUpdateStatePort(),
                successfulFindDesiredDeploymentStatePort(),
                _ -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                successfulFindActualDeploymentStatePort()
        );

        DeployServiceUseCase.DeployServiceResult result = service.deployService(
                new DeployServiceUseCase.DeployServiceCommand(SERVICE.name(), IMAGE_VERSION)
        );

        assertThat(result).isInstanceOf(DeployServiceUseCase.DeployServiceResult.DesiredStateFailure.class);
    }

    @Test
    void returns_service_not_found_when_upsert_reports_missing_service() {
        ServiceRuntimeDomainService service = new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.Found(SERVICE),
                _ -> new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.ServiceNotFound(),
                successfulUpdateStatePort(),
                successfulFindDesiredDeploymentStatePort(),
                _ -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                successfulFindActualDeploymentStatePort()
        );

        DeployServiceUseCase.DeployServiceResult result = service.deployService(
                new DeployServiceUseCase.DeployServiceCommand(SERVICE.name(), IMAGE_VERSION)
        );

        assertThat(result).isInstanceOf(DeployServiceUseCase.DeployServiceResult.ServiceNotFound.class);
    }

    @Test
    void returns_desired_state_failure_when_upsert_fails() {
        ServiceRuntimeDomainService service = new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.Found(SERVICE),
                _ -> new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Failure(),
                successfulUpdateStatePort(),
                successfulFindDesiredDeploymentStatePort(),
                _ -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                successfulFindActualDeploymentStatePort()
        );

        DeployServiceUseCase.DeployServiceResult result = service.deployService(
                new DeployServiceUseCase.DeployServiceCommand(SERVICE.name(), IMAGE_VERSION)
        );

        assertThat(result).isInstanceOf(DeployServiceUseCase.DeployServiceResult.DesiredStateFailure.class);
    }

    @Test
    void returns_docker_failure_when_deploy_port_fails() {
        ServiceRuntimeDomainService service = new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.Found(SERVICE),
                _ -> new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success(),
                successfulUpdateStatePort(),
                successfulFindDesiredDeploymentStatePort(),
                _ -> new DeployContainerPort.DeployContainerResult.Failure(),
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                successfulFindActualDeploymentStatePort()
        );

        DeployServiceUseCase.DeployServiceResult result = service.deployService(
                new DeployServiceUseCase.DeployServiceCommand(SERVICE.name(), IMAGE_VERSION)
        );

        assertThat(result).isInstanceOf(DeployServiceUseCase.DeployServiceResult.DockerFailure.class);
    }

    @Test
    void starts_service_when_state_update_and_start_port_succeed() {
        ServiceRuntimeDomainService service = new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.NotFound(),
                _ -> new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success(),
                successfulUpdateStatePort(),
                successfulFindDesiredDeploymentStatePort(),
                _ -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                successfulFindActualDeploymentStatePort()
        );

        StartServiceUseCase.StartServiceResult result = service.startService(
                new StartServiceUseCase.StartServiceCommand(SERVICE.name())
        );

        assertThat(result).isInstanceOf(StartServiceUseCase.StartServiceResult.Success.class);
    }

    @Test
    void stops_service_when_state_update_and_stop_port_succeed() {
        ServiceRuntimeDomainService service = new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.NotFound(),
                _ -> new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success(),
                successfulUpdateStatePort(),
                successfulFindDesiredDeploymentStatePort(),
                _ -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                successfulFindActualDeploymentStatePort()
        );

        StopServiceUseCase.StopServiceResult result = service.stopService(
                new StopServiceUseCase.StopServiceCommand(SERVICE.name())
        );

        assertThat(result).isInstanceOf(StopServiceUseCase.StopServiceResult.Success.class);
    }

    @Test
    void returns_status_for_each_desired_and_actual_state_combination() {
        List<StatusCase> statusCases = List.of(
                new StatusCase(DesiredDeploymentState.RUNNING, ActualDeploymentState.RUNNING, ServiceRuntimeStatus.RUNNING),
                new StatusCase(
                        DesiredDeploymentState.RUNNING,
                        ActualDeploymentState.STOPPED,
                        ServiceRuntimeStatus.EXPECTED_RUNNING_BUT_STOPPED
                ),
                new StatusCase(
                        DesiredDeploymentState.RUNNING,
                        ActualDeploymentState.MISSING,
                        ServiceRuntimeStatus.EXPECTED_RUNNING_BUT_MISSING
                ),
                new StatusCase(
                        DesiredDeploymentState.STOPPED,
                        ActualDeploymentState.RUNNING,
                        ServiceRuntimeStatus.EXPECTED_STOPPED_BUT_RUNNING
                ),
                new StatusCase(DesiredDeploymentState.STOPPED, ActualDeploymentState.STOPPED, ServiceRuntimeStatus.STOPPED),
                new StatusCase(
                        DesiredDeploymentState.STOPPED,
                        ActualDeploymentState.MISSING,
                        ServiceRuntimeStatus.EXPECTED_STOPPED_BUT_MISSING
                )
        );

        for (StatusCase statusCase : statusCases) {
            ServiceRuntimeDomainService service = serviceWithStatusPorts(
                    _ -> new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Found(
                            statusCase.desiredState()
                    ),
                    _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found(
                            statusCase.actualState()
                    )
            );

            GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult result = service.getServiceRuntimeStatus(
                    new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusCommand(SERVICE.name())
            );

            assertThat(result)
                    .as("desired %s actual %s", statusCase.desiredState(), statusCase.actualState())
                    .isEqualTo(new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult.Success(
                            statusCase.expectedStatus()
                    ));
        }
    }

    @Test
    void returns_status_for_services_without_desired_deployment() {
        List<UndeployedStatusCase> statusCases = List.of(
                new UndeployedStatusCase(ActualDeploymentState.RUNNING, ServiceRuntimeStatus.ORPHANED_RUNNING),
                new UndeployedStatusCase(ActualDeploymentState.STOPPED, ServiceRuntimeStatus.ORPHANED_STOPPED),
                new UndeployedStatusCase(ActualDeploymentState.MISSING, ServiceRuntimeStatus.NOT_DEPLOYED)
        );

        for (UndeployedStatusCase statusCase : statusCases) {
            ServiceRuntimeDomainService service = serviceWithStatusPorts(
                    _ -> new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.NotDeployed(),
                    _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found(
                            statusCase.actualState()
                    )
            );

            GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult result = service.getServiceRuntimeStatus(
                    new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusCommand(SERVICE.name())
            );

            assertThat(result)
                    .as("actual %s", statusCase.actualState())
                    .isEqualTo(new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult.Success(
                            statusCase.expectedStatus()
                    ));
        }
    }

    @Test
    void returns_duplicate_managed_containers_status_when_docker_reports_duplicates() {
        ServiceRuntimeDomainService service = serviceWithStatusPorts(
                _ -> new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Found(
                        DesiredDeploymentState.RUNNING
                ),
                _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.DuplicateManagedContainers()
        );

        GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult result = service.getServiceRuntimeStatus(
                new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusCommand(SERVICE.name())
        );

        assertThat(result).isEqualTo(new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult.Success(
                ServiceRuntimeStatus.DUPLICATE_MANAGED_CONTAINERS
        ));
    }

    @Test
    void returns_service_not_found_when_status_desired_state_lookup_reports_missing_service() {
        ServiceRuntimeDomainService service = serviceWithStatusPorts(
                _ -> new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.ServiceNotFound(),
                successfulFindActualDeploymentStatePort()
        );

        GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult result = service.getServiceRuntimeStatus(
                new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusCommand(SERVICE.name())
        );

        assertThat(result)
                .isInstanceOf(GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult.ServiceNotFound.class);
    }

    @Test
    void returns_desired_state_failure_when_status_desired_state_lookup_fails() {
        ServiceRuntimeDomainService service = serviceWithStatusPorts(
                _ -> new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Failure(),
                successfulFindActualDeploymentStatePort()
        );

        GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult result = service.getServiceRuntimeStatus(
                new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusCommand(SERVICE.name())
        );

        assertThat(result)
                .isInstanceOf(GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult.DesiredStateFailure.class);
    }

    @Test
    void returns_docker_failure_when_status_actual_state_lookup_fails() {
        ServiceRuntimeDomainService service = serviceWithStatusPorts(
                successfulFindDesiredDeploymentStatePort(),
                _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Failure()
        );

        GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult result = service.getServiceRuntimeStatus(
                new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusCommand(SERVICE.name())
        );

        assertThat(result)
                .isInstanceOf(GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult.DockerFailure.class);
    }

    private static Service service() {
        EnvironmentVariables environmentVariables = EnvironmentVariables.empty()
                .add(new EnvironmentVariables.Key("APP_ENV"), new EnvironmentVariables.Value("prod"));

        PortMappings portMappings = PortMappings.empty()
                .add(new Port(8080), new Port(80));

        VolumeMounts volumeMounts = VolumeMounts.empty()
                .add(new VolumeMount.NamedVolumeMount(
                        new VolumeMount.VolumeName("deployko_data"),
                        new VolumeMount.Target("/var/lib/deployko"),
                        false
                ));

        NetworkAttachments networkAttachments = NetworkAttachments.empty()
                .add(new NetworkAttachment(new NetworkAttachment.NetworkName("backend")));

        return new Service(
                new ServiceName("deployko-api"),
                new ImageRepository("ghcr.io/deployko/api"),
                new RuntimeConfiguration(environmentVariables, portMappings, volumeMounts, networkAttachments)
        );
    }

    private static UpdateDesiredDeploymentStatePort successfulUpdateStatePort() {
        return (_, _) -> new UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.Success();
    }

    private static FindDesiredDeploymentStatePort successfulFindDesiredDeploymentStatePort() {
        return _ -> new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Found(
                DesiredDeploymentState.RUNNING
        );
    }

    private static StartContainerPort successfulStartContainerPort() {
        return _ -> new StartContainerPort.StartContainerResult.Success();
    }

    private static StopContainerPort successfulStopContainerPort() {
        return _ -> new StopContainerPort.StopContainerResult.Success();
    }

    private static FindActualDeploymentStatePort successfulFindActualDeploymentStatePort() {
        return _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found(
                ActualDeploymentState.RUNNING
        );
    }

    private static ServiceRuntimeDomainService serviceWithStatusPorts(
            FindDesiredDeploymentStatePort findDesiredDeploymentStatePort,
            FindActualDeploymentStatePort findActualDeploymentStatePort
    ) {
        return new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.NotFound(),
                _ -> new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success(),
                successfulUpdateStatePort(),
                findDesiredDeploymentStatePort,
                _ -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                findActualDeploymentStatePort
        );
    }

    private record StatusCase(
            DesiredDeploymentState desiredState,
            ActualDeploymentState actualState,
            ServiceRuntimeStatus expectedStatus
    ) {
    }

    private record UndeployedStatusCase(
            ActualDeploymentState actualState,
            ServiceRuntimeStatus expectedStatus
    ) {
    }

    private static final class FakeUpsertDesiredDeploymentPort implements UpsertDesiredDeploymentPort {

        private final UpsertDesiredDeploymentResult result;
        private final List<DesiredDeployment> upsertedDeployments = new ArrayList<>();

        private FakeUpsertDesiredDeploymentPort(UpsertDesiredDeploymentResult result) {
            this.result = result;
        }

        @Override
        public UpsertDesiredDeploymentResult upsert(DesiredDeployment desiredDeployment) {
            upsertedDeployments.add(desiredDeployment);
            return result;
        }
    }

    private static final class FakeDeployContainerPort implements DeployContainerPort {

        private final DeployContainerResult result;
        private final List<DesiredDeployment> deployedDeployments = new ArrayList<>();

        private FakeDeployContainerPort(DeployContainerResult result) {
            this.result = result;
        }

        @Override
        public DeployContainerResult deploy(DesiredDeployment desiredDeployment) {
            deployedDeployments.add(desiredDeployment);
            return result;
        }
    }
}
