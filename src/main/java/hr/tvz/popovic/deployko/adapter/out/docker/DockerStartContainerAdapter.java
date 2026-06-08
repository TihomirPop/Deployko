package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.StartContainerPort;

import java.util.Objects;

public class DockerStartContainerAdapter implements StartContainerPort {

    private final DockerJavaContainerClient dockerContainerClient;

    public DockerStartContainerAdapter(DockerClient dockerClient) {
        this.dockerContainerClient = new DockerJavaContainerClient(dockerClient);
    }

    @Override
    public StartContainerResult start(ServiceName serviceName) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");

        try {
            return startSelectedContainer(serviceName);
        } catch (DockerException _) {
            return new StartContainerResult.Failure();
        }
    }

    private StartContainerResult startSelectedContainer(ServiceName serviceName) {
        return switch (selectContainer(serviceName)) {
            case DockerManagedContainerSelector.ManagedContainerSelection.Found found -> {
                dockerContainerClient.startContainer(found.container().getId());
                yield new StartContainerResult.Success();
            }
            case DockerManagedContainerSelector.ManagedContainerSelection.Missing _ ->
                    new StartContainerResult.MissingContainer();
            case DockerManagedContainerSelector.ManagedContainerSelection.Duplicate _ ->
                    new StartContainerResult.DuplicateManagedContainers();
        };
    }

    private DockerManagedContainerSelector.ManagedContainerSelection selectContainer(ServiceName serviceName) {
        return DockerManagedContainerSelector.selectSingle(dockerContainerClient.listManagedContainers(serviceName));
    }
}
