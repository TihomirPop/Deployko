package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Container;
import hr.tvz.popovic.deployko.application.domain.model.ActualDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.FindActualDeploymentStatePort;

import java.util.Objects;

public class DockerFindActualDeploymentStateAdapter implements FindActualDeploymentStatePort {

    private static final String RUNNING_DOCKER_STATE = "running";

    private final DockerJavaContainerClient dockerContainerClient;

    public DockerFindActualDeploymentStateAdapter(DockerClient dockerClient) {
        this.dockerContainerClient = new DockerJavaContainerClient(dockerClient);
    }

    @Override
    public FindActualDeploymentStateResult findActualState(ServiceName serviceName) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");

        try {
            return switch (DockerManagedContainerSelector.selectSingle(
                    dockerContainerClient.listManagedContainers(serviceName)
            )) {
                case DockerManagedContainerSelector.ManagedContainerSelection.Found found ->
                        new FindActualDeploymentStateResult.Found(
                                actualState(found.container()),
                                dockerContainerClient.restartCount(found.container().getId())
                        );
                case DockerManagedContainerSelector.ManagedContainerSelection.Missing _ ->
                        new FindActualDeploymentStateResult.Found(ActualDeploymentState.MISSING);
                case DockerManagedContainerSelector.ManagedContainerSelection.Duplicate _ ->
                        new FindActualDeploymentStateResult.DuplicateManagedContainers();
            };
        } catch (DockerException _) {
            return new FindActualDeploymentStateResult.Failure();
        }
    }

    private static ActualDeploymentState actualState(Container container) {
        return RUNNING_DOCKER_STATE.equals(container.getState())
                ? ActualDeploymentState.RUNNING
                : ActualDeploymentState.STOPPED;
    }
}
