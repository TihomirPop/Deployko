package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;
import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachment;
import hr.tvz.popovic.deployko.application.port.out.DeployContainerPort;

import java.util.Objects;

public class DockerDeployContainerAdapter implements DeployContainerPort {

    private final DockerDeploymentClient dockerDeploymentClient;

    public DockerDeployContainerAdapter(DockerClient dockerClient) {
        this(new DockerJavaDeploymentClient(dockerClient));
    }

    DockerDeployContainerAdapter(DockerDeploymentClient dockerDeploymentClient) {
        this.dockerDeploymentClient = Objects.requireNonNull(
                dockerDeploymentClient,
                "dockerDeploymentClient must not be null"
        );
    }

    @Override
    public DeployContainerResult deploy(DesiredDeployment desiredDeployment) {
        Objects.requireNonNull(desiredDeployment, "desiredDeployment must not be null");

        try {
            return deployContainer(desiredDeployment);
        } catch (DockerException _) {
            return new DeployContainerResult.Failure();
        }
    }

    private DeployContainerResult deployContainer(DesiredDeployment desiredDeployment) {
        String containerId = dockerDeploymentClient.createContainer(desiredDeployment);

        for (NetworkAttachment networkAttachment : desiredDeployment.runtimeConfiguration().networkAttachments().asMap().values()) {
            dockerDeploymentClient.connectToNetwork(containerId, networkAttachment.networkName().value());
        }

        dockerDeploymentClient.startContainer(containerId);
        return new DeployContainerResult.Success();
    }
}
