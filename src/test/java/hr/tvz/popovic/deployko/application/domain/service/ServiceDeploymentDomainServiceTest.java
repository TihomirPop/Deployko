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
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMounts;
import hr.tvz.popovic.deployko.application.port.in.ServiceDeploymentUseCase;
import hr.tvz.popovic.deployko.application.port.out.DeployContainerPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceDefinitionPort;
import hr.tvz.popovic.deployko.application.port.out.UpsertDesiredDeploymentPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceDeploymentDomainServiceTest {

    private static final ServiceName SERVICE_NAME = new ServiceName("deployko-api");
    private static final ImageVersion IMAGE_VERSION = new ImageVersion("1.0.0");
    private static final RuntimeConfiguration RUNTIME_CONFIGURATION = runtimeConfiguration();

    @Test
    void deploys_service_when_definition_exists_and_ports_succeed() {
        FakeFindServiceDefinitionPort findServiceDefinitionPort = new FakeFindServiceDefinitionPort(
                new FindServiceDefinitionPort.FindServiceDefinitionResult.Found(
                        SERVICE_NAME,
                        new ImageRepository("ghcr.io/deployko/api")
                )
        );
        FakeUpsertDesiredDeploymentPort upsertDesiredDeploymentPort = new FakeUpsertDesiredDeploymentPort(
                new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success()
        );
        FakeDeployContainerPort deployContainerPort = new FakeDeployContainerPort(
                new DeployContainerPort.DeployContainerResult.Success()
        );
        ServiceDeploymentDomainService service = new ServiceDeploymentDomainService(
                findServiceDefinitionPort,
                upsertDesiredDeploymentPort,
                deployContainerPort
        );

        ServiceDeploymentUseCase.DeployServiceResult result = service.deployService(
                new ServiceDeploymentUseCase.DeployServiceCommand(
                        SERVICE_NAME,
                        IMAGE_VERSION,
                        RUNTIME_CONFIGURATION
                )
        );

        assertThat(result).isInstanceOf(ServiceDeploymentUseCase.DeployServiceResult.Success.class);
        assertThat(upsertDesiredDeploymentPort.upsertedDeployments).hasSize(1);
        assertThat(upsertDesiredDeploymentPort.upsertedDeployments.getFirst()).isEqualTo(new DesiredDeployment(
                SERVICE_NAME,
                new ImageRepository("ghcr.io/deployko/api"),
                IMAGE_VERSION,
                RUNTIME_CONFIGURATION,
                DesiredDeploymentState.RUNNING
        ));
        assertThat(deployContainerPort.deployedDeployments).containsExactly(upsertDesiredDeploymentPort.upsertedDeployments.getFirst());
    }

    @Test
    void returns_service_not_found_when_service_definition_is_missing() {
        ServiceDeploymentDomainService service = new ServiceDeploymentDomainService(
                new FakeFindServiceDefinitionPort(new FindServiceDefinitionPort.FindServiceDefinitionResult.NotFound()),
                new FakeUpsertDesiredDeploymentPort(new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success()),
                new FakeDeployContainerPort(new DeployContainerPort.DeployContainerResult.Success())
        );

        ServiceDeploymentUseCase.DeployServiceResult result = service.deployService(
                new ServiceDeploymentUseCase.DeployServiceCommand(
                        SERVICE_NAME,
                        IMAGE_VERSION,
                        RUNTIME_CONFIGURATION
                )
        );

        assertThat(result).isInstanceOf(ServiceDeploymentUseCase.DeployServiceResult.ServiceNotFound.class);
    }

    @Test
    void returns_desired_state_failure_when_service_definition_lookup_fails() {
        ServiceDeploymentDomainService service = new ServiceDeploymentDomainService(
                new FakeFindServiceDefinitionPort(new FindServiceDefinitionPort.FindServiceDefinitionResult.Failure()),
                new FakeUpsertDesiredDeploymentPort(new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success()),
                new FakeDeployContainerPort(new DeployContainerPort.DeployContainerResult.Success())
        );

        ServiceDeploymentUseCase.DeployServiceResult result = service.deployService(
                new ServiceDeploymentUseCase.DeployServiceCommand(
                        SERVICE_NAME,
                        IMAGE_VERSION,
                        RUNTIME_CONFIGURATION
                )
        );

        assertThat(result).isInstanceOf(ServiceDeploymentUseCase.DeployServiceResult.DesiredStateFailure.class);
    }

    @Test
    void returns_service_not_found_when_upsert_reports_missing_service() {
        ServiceDeploymentDomainService service = new ServiceDeploymentDomainService(
                new FakeFindServiceDefinitionPort(new FindServiceDefinitionPort.FindServiceDefinitionResult.Found(
                        SERVICE_NAME,
                        new ImageRepository("ghcr.io/deployko/api")
                )),
                new FakeUpsertDesiredDeploymentPort(
                        new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.ServiceNotFound()
                ),
                new FakeDeployContainerPort(new DeployContainerPort.DeployContainerResult.Success())
        );

        ServiceDeploymentUseCase.DeployServiceResult result = service.deployService(
                new ServiceDeploymentUseCase.DeployServiceCommand(
                        SERVICE_NAME,
                        IMAGE_VERSION,
                        RUNTIME_CONFIGURATION
                )
        );

        assertThat(result).isInstanceOf(ServiceDeploymentUseCase.DeployServiceResult.ServiceNotFound.class);
    }

    @Test
    void returns_desired_state_failure_when_upsert_fails() {
        ServiceDeploymentDomainService service = new ServiceDeploymentDomainService(
                new FakeFindServiceDefinitionPort(new FindServiceDefinitionPort.FindServiceDefinitionResult.Found(
                        SERVICE_NAME,
                        new ImageRepository("ghcr.io/deployko/api")
                )),
                new FakeUpsertDesiredDeploymentPort(
                        new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Failure()
                ),
                new FakeDeployContainerPort(new DeployContainerPort.DeployContainerResult.Success())
        );

        ServiceDeploymentUseCase.DeployServiceResult result = service.deployService(
                new ServiceDeploymentUseCase.DeployServiceCommand(
                        SERVICE_NAME,
                        IMAGE_VERSION,
                        RUNTIME_CONFIGURATION
                )
        );

        assertThat(result).isInstanceOf(ServiceDeploymentUseCase.DeployServiceResult.DesiredStateFailure.class);
    }

    @Test
    void returns_docker_failure_when_deploy_port_fails() {
        ServiceDeploymentDomainService service = new ServiceDeploymentDomainService(
                new FakeFindServiceDefinitionPort(new FindServiceDefinitionPort.FindServiceDefinitionResult.Found(
                        SERVICE_NAME,
                        new ImageRepository("ghcr.io/deployko/api")
                )),
                new FakeUpsertDesiredDeploymentPort(
                        new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success()
                ),
                new FakeDeployContainerPort(new DeployContainerPort.DeployContainerResult.Failure())
        );

        ServiceDeploymentUseCase.DeployServiceResult result = service.deployService(
                new ServiceDeploymentUseCase.DeployServiceCommand(
                        SERVICE_NAME,
                        IMAGE_VERSION,
                        RUNTIME_CONFIGURATION
                )
        );

        assertThat(result).isInstanceOf(ServiceDeploymentUseCase.DeployServiceResult.DockerFailure.class);
    }

    @Test
    void start_service_is_not_implemented_yet() {
        ServiceDeploymentDomainService service = new ServiceDeploymentDomainService(
                new FakeFindServiceDefinitionPort(new FindServiceDefinitionPort.FindServiceDefinitionResult.NotFound()),
                new FakeUpsertDesiredDeploymentPort(new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success()),
                new FakeDeployContainerPort(new DeployContainerPort.DeployContainerResult.Success())
        );

        assertThatThrownBy(() -> service.startService(new ServiceDeploymentUseCase.StartServiceCommand(SERVICE_NAME)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("start service is not implemented yet");
    }

    @Test
    void stop_service_is_not_implemented_yet() {
        ServiceDeploymentDomainService service = new ServiceDeploymentDomainService(
                new FakeFindServiceDefinitionPort(new FindServiceDefinitionPort.FindServiceDefinitionResult.NotFound()),
                new FakeUpsertDesiredDeploymentPort(new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success()),
                new FakeDeployContainerPort(new DeployContainerPort.DeployContainerResult.Success())
        );

        assertThatThrownBy(() -> service.stopService(new ServiceDeploymentUseCase.StopServiceCommand(SERVICE_NAME)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("stop service is not implemented yet");
    }

    private static RuntimeConfiguration runtimeConfiguration() {
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

        return new RuntimeConfiguration(environmentVariables, portMappings, volumeMounts, networkAttachments);
    }

    private static final class FakeFindServiceDefinitionPort implements FindServiceDefinitionPort {

        private final FindServiceDefinitionResult result;

        private FakeFindServiceDefinitionPort(FindServiceDefinitionResult result) {
            this.result = result;
        }

        @Override
        public FindServiceDefinitionResult findByName(ServiceName serviceName) {
            assertThat(serviceName).isEqualTo(SERVICE_NAME);
            return result;
        }
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
