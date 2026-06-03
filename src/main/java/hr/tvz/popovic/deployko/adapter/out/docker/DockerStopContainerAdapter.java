package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.StopContainerPort;

import java.util.Objects;

public class DockerStopContainerAdapter implements StopContainerPort {

    private final DockerContainerClient dockerContainerClient;

    public DockerStopContainerAdapter(DockerClient dockerClient) {
        this(new DockerJavaContainerClient(dockerClient));
    }

    DockerStopContainerAdapter(DockerContainerClient dockerContainerClient) {
        this.dockerContainerClient = Objects.requireNonNull(
                dockerContainerClient,
                "dockerContainerClient must not be null"
        );
    }

    @Override
    public StopContainerResult stop(ServiceName serviceName) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");

        try {
            return stopSelectedContainer(serviceName);
        } catch (DockerException _) {
            return new StopContainerResult.Failure();
        }
    }

    private StopContainerResult stopSelectedContainer(ServiceName serviceName) {
        return switch (selectContainer(serviceName)) {
            case DockerManagedContainerSelector.ManagedContainerSelection.Found found -> {
                dockerContainerClient.stopContainer(found.container().getId());
                yield new StopContainerResult.Success();
            }
            case DockerManagedContainerSelector.ManagedContainerSelection.Missing _ ->
                    new StopContainerResult.MissingContainer();
            case DockerManagedContainerSelector.ManagedContainerSelection.Duplicate _ ->
                    new StopContainerResult.DuplicateManagedContainers();
        };
    }

    private DockerManagedContainerSelector.ManagedContainerSelection selectContainer(ServiceName serviceName) {
        return DockerManagedContainerSelector.selectSingle(dockerContainerClient.listManagedContainers(serviceName));
    }
}
