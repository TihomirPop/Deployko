package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;

import java.util.Objects;

class DockerJavaDeploymentClient implements DockerDeploymentClient {

    private final DockerClient dockerClient;

    DockerJavaDeploymentClient(DockerClient dockerClient) {
        this.dockerClient = Objects.requireNonNull(dockerClient, "dockerClient must not be null");
    }

    @Override
    public String createContainer(DesiredDeployment desiredDeployment) {
        Objects.requireNonNull(desiredDeployment, "desiredDeployment must not be null");

        CreateContainerCmd command = dockerClient.createContainerCmd(DockerDeploymentMetadata.imageReference(desiredDeployment))
                .withName(DockerDeploymentMetadata.containerName(desiredDeployment))
                .withLabels(DockerDeploymentMetadata.labels(desiredDeployment))
                .withEnv(DockerEnvironmentVariables.from(desiredDeployment.runtimeConfiguration().environmentVariables()))
                .withHostConfig(
                        HostConfig.newHostConfig()
                                .withBinds(DockerVolumeBinds.from(desiredDeployment.runtimeConfiguration().volumeMounts()))
                                .withPortBindings(DockerPortBindings.from(desiredDeployment.runtimeConfiguration().portMappings()))
                );

        CreateContainerResponse response = command.exec();
        return response.getId();
    }

    @Override
    public void connectToNetwork(String containerId, String networkName) {
        Objects.requireNonNull(containerId, "containerId must not be null");
        Objects.requireNonNull(networkName, "networkName must not be null");

        dockerClient.connectToNetworkCmd()
                .withContainerId(containerId)
                .withNetworkId(networkName)
                .exec();
    }

    @Override
    public void startContainer(String containerId) {
        Objects.requireNonNull(containerId, "containerId must not be null");

        dockerClient.startContainerCmd(containerId).exec();
    }
}
