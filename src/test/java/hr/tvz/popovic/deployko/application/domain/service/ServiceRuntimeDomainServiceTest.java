package hr.tvz.popovic.deployko.application.domain.service;

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
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMounts;
import hr.tvz.popovic.deployko.application.port.in.DeployServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.StartServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.StopServiceUseCase;
import hr.tvz.popovic.deployko.application.port.out.DeployContainerPort;
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
                deployContainerPort,
                successfulStartContainerPort(),
                successfulStopContainerPort()
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
                _ -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort()
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
                _ -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort()
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
                _ -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort()
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
                _ -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort()
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
                _ -> new DeployContainerPort.DeployContainerResult.Failure(),
                successfulStartContainerPort(),
                successfulStopContainerPort()
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
                _ -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort()
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
                _ -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort()
        );

        StopServiceUseCase.StopServiceResult result = service.stopService(
                new StopServiceUseCase.StopServiceCommand(SERVICE.name())
        );

        assertThat(result).isInstanceOf(StopServiceUseCase.StopServiceResult.Success.class);
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

    private static StartContainerPort successfulStartContainerPort() {
        return _ -> new StartContainerPort.StartContainerResult.Success();
    }

    private static StopContainerPort successfulStopContainerPort() {
        return _ -> new StopContainerPort.StopContainerResult.Success();
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
