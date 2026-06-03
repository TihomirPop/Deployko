package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.exception.DockerException;
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
import hr.tvz.popovic.deployko.application.port.out.DeployContainerPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DockerDeployContainerAdapterTest {

    private static final DesiredDeployment DESIRED_DEPLOYMENT = desiredDeployment();

    private final FakeDockerDeploymentClient dockerDeploymentClient = new FakeDockerDeploymentClient();
    private final DockerDeployContainerAdapter adapter = new DockerDeployContainerAdapter(dockerDeploymentClient);

    @Test
    void deploys_container_with_network_attachments_and_starts_it() {
        dockerDeploymentClient.createdContainerId = "container-1";

        DeployContainerPort.DeployContainerResult result = adapter.deploy(DESIRED_DEPLOYMENT);

        assertThat(result).isInstanceOf(DeployContainerPort.DeployContainerResult.Success.class);
        assertThat(dockerDeploymentClient.createdDesiredDeployment).isEqualTo(DESIRED_DEPLOYMENT);
        assertThat(dockerDeploymentClient.connectedNetworks)
                .containsExactly("backend", "observability");
        assertThat(dockerDeploymentClient.startedContainerIds).containsExactly("container-1");
    }

    @Test
    void deploys_container_without_network_attachments() {
        dockerDeploymentClient.createdContainerId = "container-1";
        DesiredDeployment deployment = new DesiredDeployment(
                DESIRED_DEPLOYMENT.serviceName(),
                DESIRED_DEPLOYMENT.imageRepository(),
                DESIRED_DEPLOYMENT.imageVersion(),
                RuntimeConfiguration.empty(),
                DESIRED_DEPLOYMENT.desiredState()
        );

        DeployContainerPort.DeployContainerResult result = adapter.deploy(deployment);

        assertThat(result).isInstanceOf(DeployContainerPort.DeployContainerResult.Success.class);
        assertThat(dockerDeploymentClient.connectedNetworks).isEmpty();
        assertThat(dockerDeploymentClient.startedContainerIds).containsExactly("container-1");
    }

    @Test
    void returns_failure_when_container_creation_fails() {
        dockerDeploymentClient.createFailure = new DockerException("docker unavailable", 500);

        DeployContainerPort.DeployContainerResult result = adapter.deploy(DESIRED_DEPLOYMENT);

        assertThat(result).isInstanceOf(DeployContainerPort.DeployContainerResult.Failure.class);
        assertThat(dockerDeploymentClient.connectedNetworks).isEmpty();
        assertThat(dockerDeploymentClient.startedContainerIds).isEmpty();
    }

    @Test
    void returns_failure_when_network_attachment_fails() {
        dockerDeploymentClient.createdContainerId = "container-1";
        dockerDeploymentClient.connectFailure = new DockerException("docker unavailable", 500);

        DeployContainerPort.DeployContainerResult result = adapter.deploy(DESIRED_DEPLOYMENT);

        assertThat(result).isInstanceOf(DeployContainerPort.DeployContainerResult.Failure.class);
        assertThat(dockerDeploymentClient.connectedNetworks).containsExactly("backend");
        assertThat(dockerDeploymentClient.startedContainerIds).isEmpty();
    }

    @Test
    void returns_failure_when_starting_container_fails() {
        dockerDeploymentClient.createdContainerId = "container-1";
        dockerDeploymentClient.startFailure = new DockerException("docker unavailable", 500);

        DeployContainerPort.DeployContainerResult result = adapter.deploy(DESIRED_DEPLOYMENT);

        assertThat(result).isInstanceOf(DeployContainerPort.DeployContainerResult.Failure.class);
        assertThat(dockerDeploymentClient.connectedNetworks).containsExactly("backend", "observability");
        assertThat(dockerDeploymentClient.startedContainerIds).containsExactly("container-1");
    }

    private static DesiredDeployment desiredDeployment() {
        EnvironmentVariables environmentVariables = EnvironmentVariables.empty()
                .add(new EnvironmentVariables.Key("APP_ENV"), new EnvironmentVariables.Value("prod"))
                .add(new EnvironmentVariables.Key("JAVA_OPTS"), new EnvironmentVariables.Value("-Xmx512m"));

        PortMappings portMappings = PortMappings.empty()
                .add(new Port(8080), new Port(80))
                .add(new Port(8443, Port.Protocol.UDP), new Port(443, Port.Protocol.UDP));

        VolumeMounts volumeMounts = VolumeMounts.empty()
                .add(new VolumeMount.BindMount(
                        new VolumeMount.HostPath("/opt/deployko/config"),
                        new VolumeMount.Target("/app/config"),
                        true
                ))
                .add(new VolumeMount.NamedVolumeMount(
                        new VolumeMount.VolumeName("deployko_data"),
                        new VolumeMount.Target("/var/lib/deployko"),
                        false
                ));

        NetworkAttachments networkAttachments = NetworkAttachments.empty()
                .add(new NetworkAttachment(new NetworkAttachment.NetworkName("backend")))
                .add(new NetworkAttachment(new NetworkAttachment.NetworkName("observability")));

        return new DesiredDeployment(
                new ServiceName("deployko-api"),
                new ImageRepository("ghcr.io/deployko/api"),
                new ImageVersion("1.0.0"),
                new RuntimeConfiguration(environmentVariables, portMappings, volumeMounts, networkAttachments),
                DesiredDeploymentState.RUNNING
        );
    }

    private static final class FakeDockerDeploymentClient implements DockerDeploymentClient {

        private DesiredDeployment createdDesiredDeployment;
        private String createdContainerId;
        private final List<String> connectedNetworks = new ArrayList<>();
        private final List<String> startedContainerIds = new ArrayList<>();
        private DockerException createFailure;
        private DockerException connectFailure;
        private DockerException startFailure;

        @Override
        public String createContainer(DesiredDeployment desiredDeployment) {
            this.createdDesiredDeployment = desiredDeployment;

            if (createFailure != null) {
                throw createFailure;
            }

            return createdContainerId;
        }

        @Override
        public void connectToNetwork(String containerId, String networkName) {
            assertThat(containerId).isEqualTo(createdContainerId);
            connectedNetworks.add(networkName);

            if (connectFailure != null) {
                throw connectFailure;
            }
        }

        @Override
        public void startContainer(String containerId) {
            assertThat(containerId).isEqualTo(createdContainerId);
            startedContainerIds.add(containerId);

            if (startFailure != null) {
                throw startFailure;
            }
        }
    }
}
