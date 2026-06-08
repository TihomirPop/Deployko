package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Container;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

import java.util.List;
import java.util.Objects;

class DockerJavaContainerClient {

    private final DockerClient dockerClient;

    DockerJavaContainerClient(DockerClient dockerClient) {
        this.dockerClient = Objects.requireNonNull(dockerClient, "dockerClient must not be null");
    }

    public List<Container> listManagedContainers(ServiceName serviceName) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");

        ListContainersCmd command = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withLabelFilter(DockerManagedContainerSelector.labelFilter(serviceName));

        return command.exec();
    }

    public int restartCount(String containerId) {
        Objects.requireNonNull(containerId, "containerId must not be null");

        Integer restartCount = dockerClient
                .inspectContainerCmd(containerId)
                .exec()
                .getRestartCount();

        return restartCount == null ? 0 : restartCount;
    }

    public void startContainer(String containerId) {
        Objects.requireNonNull(containerId, "containerId must not be null");

        dockerClient.startContainerCmd(containerId).exec();
    }

    public void stopContainer(String containerId) {
        Objects.requireNonNull(containerId, "containerId must not be null");

        try {
            dockerClient.stopContainerCmd(containerId).exec();
        } catch (NotModifiedException _) {
        }
    }

    public void removeContainer(String containerId) {
        Objects.requireNonNull(containerId, "containerId must not be null");

        try {
            dockerClient.removeContainerCmd(containerId).exec();
        } catch (NotFoundException _) {
        }
    }
}
