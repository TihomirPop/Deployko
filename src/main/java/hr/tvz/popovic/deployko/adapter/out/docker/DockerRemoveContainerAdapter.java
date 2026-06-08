package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.RemoveContainerPort;

import java.util.Objects;

public class DockerRemoveContainerAdapter implements RemoveContainerPort {

    private final DockerJavaContainerClient dockerContainerClient;

    public DockerRemoveContainerAdapter(DockerClient dockerClient) {
        this.dockerContainerClient = new DockerJavaContainerClient(dockerClient);
    }

    @Override
    public RemoveContainerResult remove(ServiceName serviceName) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");

        try {
            return removeSelectedContainer(serviceName);
        } catch (DockerException _) {
            return new RemoveContainerResult.Failure();
        }
    }

    private RemoveContainerResult removeSelectedContainer(ServiceName serviceName) {
        return switch (selectContainer(serviceName)) {
            case DockerManagedContainerSelector.ManagedContainerSelection.Found found -> {
                dockerContainerClient.stopContainer(found.container().getId());
                dockerContainerClient.removeContainer(found.container().getId());
                yield new RemoveContainerResult.Success();
            }
            case DockerManagedContainerSelector.ManagedContainerSelection.Missing _ ->
                    new RemoveContainerResult.MissingContainer();
            case DockerManagedContainerSelector.ManagedContainerSelection.Duplicate _ ->
                    new RemoveContainerResult.DuplicateManagedContainers();
        };
    }

    private DockerManagedContainerSelector.ManagedContainerSelection selectContainer(ServiceName serviceName) {
        return DockerManagedContainerSelector.selectSingle(dockerContainerClient.listManagedContainers(serviceName));
    }
}
