package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;
import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachment;
import hr.tvz.popovic.deployko.application.port.out.DeployContainerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class DockerDeployContainerAdapter implements DeployContainerPort {

    private static final Logger log = LoggerFactory.getLogger(DockerDeployContainerAdapter.class);
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
    public DeployContainerResult deploy(DesiredDeployment desiredDeployment, DeploymentId deploymentId) {
        Objects.requireNonNull(desiredDeployment, "desiredDeployment must not be null");
        Objects.requireNonNull(deploymentId, "deploymentId must not be null");

        try {
            return deployContainer(desiredDeployment, deploymentId);
        } catch (DockerException e) {
            log.error("Exception occurred while trying to deploy docker container", e);
            return new DeployContainerResult.Failure();
        }
    }

    private DeployContainerResult deployContainer(DesiredDeployment desiredDeployment, DeploymentId deploymentId) {
        dockerDeploymentClient.removeContainer(desiredDeployment);

        String containerId = dockerDeploymentClient.createContainer(desiredDeployment, deploymentId);

        for (NetworkAttachment networkAttachment : desiredDeployment.runtimeConfiguration().networkAttachments().asMap().values()) {
            dockerDeploymentClient.connectToNetwork(containerId, networkAttachment.networkName().value());
        }

        dockerDeploymentClient.startContainer(containerId);
        return new DeployContainerResult.Success();
    }
}
