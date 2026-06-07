package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Container;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.StartContainerPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DockerStartContainerAdapterTest {

    private static final ServiceName SERVICE_NAME = new ServiceName("deployko-api");

    private final FakeDockerContainerClient dockerContainerClient = new FakeDockerContainerClient();
    private final DockerStartContainerAdapter adapter = new DockerStartContainerAdapter(dockerContainerClient);

    @Test
    void starts_single_managed_container_for_service() {
        Container container = container("container-1");
        dockerContainerClient.containers = List.of(container);

        StartContainerPort.StartContainerResult result = adapter.start(SERVICE_NAME);

        assertThat(result).isInstanceOf(StartContainerPort.StartContainerResult.Success.class);
        assertThat(dockerContainerClient.startedContainerIds).containsExactly("container-1");
    }

    @Test
    void returns_missing_when_no_managed_container_exists() {
        dockerContainerClient.containers = List.of();

        StartContainerPort.StartContainerResult result = adapter.start(SERVICE_NAME);

        assertThat(result).isInstanceOf(StartContainerPort.StartContainerResult.MissingContainer.class);
        assertThat(dockerContainerClient.startedContainerIds).isEmpty();
    }

    @Test
    void returns_duplicate_when_multiple_managed_containers_exist() {
        dockerContainerClient.containers = List.of(container("container-1"), container("container-2"));

        StartContainerPort.StartContainerResult result = adapter.start(SERVICE_NAME);

        assertThat(result).isInstanceOf(StartContainerPort.StartContainerResult.DuplicateManagedContainers.class);
        assertThat(dockerContainerClient.startedContainerIds).isEmpty();
    }

    @Test
    void returns_failure_when_listing_containers_fails() {
        dockerContainerClient.listFailure = new DockerException("docker unavailable", 500);

        StartContainerPort.StartContainerResult result = adapter.start(SERVICE_NAME);

        assertThat(result).isInstanceOf(StartContainerPort.StartContainerResult.Failure.class);
    }

    @Test
    void returns_failure_when_starting_container_fails() {
        Container container = container("container-1");
        dockerContainerClient.containers = List.of(container);
        dockerContainerClient.startFailure = new DockerException("docker unavailable", 500);

        StartContainerPort.StartContainerResult result = adapter.start(SERVICE_NAME);

        assertThat(result).isInstanceOf(StartContainerPort.StartContainerResult.Failure.class);
        assertThat(dockerContainerClient.startedContainerIds).containsExactly("container-1");
    }

    private static Container container(String id) {
        return new TestContainer(id);
    }

    private static final class FakeDockerContainerClient implements DockerContainerClient {

        private List<Container> containers = List.of();
        private List<String> startedContainerIds = new ArrayList<>();
        private DockerException listFailure;
        private DockerException startFailure;

        @Override
        public List<Container> listManagedContainers(ServiceName serviceName) {
            assertThat(serviceName).isEqualTo(SERVICE_NAME);

            if (listFailure != null) {
                throw listFailure;
            }

            return containers;
        }

        @Override
        public int restartCount(String containerId) {
            return 0;
        }

        @Override
        public void startContainer(String containerId) {
            startedContainerIds.add(containerId);

            if (startFailure != null) {
                throw startFailure;
            }
        }

        @Override
        public void stopContainer(String containerId) {
        }

        @Override
        public void removeContainer(String containerId) {
        }
    }

    private static final class TestContainer extends Container {

        private final String id;

        private TestContainer(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }
    }
}
