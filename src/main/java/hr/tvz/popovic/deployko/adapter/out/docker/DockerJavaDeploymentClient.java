package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

class DockerJavaDeploymentClient implements DockerDeploymentClient {

    private final DockerClient dockerClient;

    DockerJavaDeploymentClient(DockerClient dockerClient) {
        this.dockerClient = Objects.requireNonNull(dockerClient, "dockerClient must not be null");
    }

    @Override
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

    @Override
    public String createContainer(DesiredDeployment desiredDeployment) {
        Objects.requireNonNull(desiredDeployment, "desiredDeployment must not be null");

        String image = DockerDeploymentMetadata.imageReference(desiredDeployment);
        ensureImageExists(image);

        CreateContainerCmd command = dockerClient.createContainerCmd(image)
                .withName(DockerDeploymentMetadata.containerName(desiredDeployment))
                .withLabels(DockerDeploymentMetadata.labels(desiredDeployment))
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

    private void ensureImageExists(String imageReference) {
        try {
            dockerClient.inspectImageCmd(imageReference).exec();
        } catch (NotFoundException imageMissing) {
            try {
                dockerClient.pullImageCmd(imageReference)
                        .exec(new PullImageResultCallback())
                        .awaitCompletion(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DockerException("Interrupted while pulling image " + imageReference, 500, e);
            }
        }
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
