package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;

import java.util.Objects;

class DockerJavaDeploymentClient {

    private final DockerClient dockerClient;

    DockerJavaDeploymentClient(DockerClient dockerClient) {
        this.dockerClient = Objects.requireNonNull(dockerClient, "dockerClient must not be null");
    }

    public void removeContainer(DesiredDeployment desiredDeployment) {
        Objects.requireNonNull(desiredDeployment, "desiredDeployment must not be null");

        String containerName = DockerDeploymentMetadata.containerName(desiredDeployment);
        try {
            dockerClient.stopContainerCmd(containerName).exec();
        } catch (NotFoundException | NotModifiedException _) {
        }
        try {
            dockerClient.removeContainerCmd(containerName).exec();
        } catch (NotFoundException _) {
        }
    }

    public String createContainer(DesiredDeployment desiredDeployment, DeploymentId deploymentId) {
        Objects.requireNonNull(desiredDeployment, "desiredDeployment must not be null");
        Objects.requireNonNull(deploymentId, "deploymentId must not be null");

        String image = DockerDeploymentMetadata.imageReference(desiredDeployment);

        CreateContainerCmd command = dockerClient.createContainerCmd(image)
                .withName(DockerDeploymentMetadata.containerName(desiredDeployment))
                .withLabels(DockerDeploymentMetadata.labels(desiredDeployment, deploymentId))
                .withEnv(DockerEnvironmentVariables.from(desiredDeployment.runtimeConfiguration().environmentVariables()))
                .withHostConfig(
                        HostConfig.newHostConfig()
                                .withBinds(DockerVolumeBinds.from(desiredDeployment.runtimeConfiguration().volumeMounts()))
                                .withPortBindings(DockerPortBindings.from(desiredDeployment.runtimeConfiguration().portMappings()))
                                .withRestartPolicy(RestartPolicy.unlessStoppedRestart())
                );

        CreateContainerResponse response = command.exec();
        return response.getId();
    }

    public void connectToNetwork(String containerId, String networkName) {
        Objects.requireNonNull(containerId, "containerId must not be null");
        Objects.requireNonNull(networkName, "networkName must not be null");

        dockerClient.connectToNetworkCmd()
                .withContainerId(containerId)
                .withNetworkId(networkName)
                .exec();
    }

    public void startContainer(String containerId) {
        Objects.requireNonNull(containerId, "containerId must not be null");

        dockerClient.startContainerCmd(containerId).exec();
    }
}
