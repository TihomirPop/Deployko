package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.model.Container;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

import java.util.List;
import java.util.Objects;

class DockerJavaContainerClient implements DockerContainerClient {

    private final DockerClient dockerClient;

    DockerJavaContainerClient(DockerClient dockerClient) {
        this.dockerClient = Objects.requireNonNull(dockerClient, "dockerClient must not be null");
    }

    @Override
    public List<Container> listManagedContainers(ServiceName serviceName) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");

        ListContainersCmd command = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withLabelFilter(DockerManagedContainerSelector.labelFilter(serviceName));

        return command.exec();
    }

    @Override
    public void startContainer(String containerId) {
        Objects.requireNonNull(containerId, "containerId must not be null");

        dockerClient.startContainerCmd(containerId).exec();
    }

    @Override
    public void stopContainer(String containerId) {
        Objects.requireNonNull(containerId, "containerId must not be null");

        dockerClient.stopContainerCmd(containerId).exec();
    }
}
