package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Container;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.RemoveContainerPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DockerRemoveContainerAdapterTest {

    private static final ServiceName SERVICE_NAME = new ServiceName("deployko-api");

    private final FakeDockerContainerClient dockerContainerClient = new FakeDockerContainerClient();
    private final DockerRemoveContainerAdapter adapter = new DockerRemoveContainerAdapter(dockerContainerClient);

    @Test
    void stops_and_removes_single_managed_container_for_service() {
        dockerContainerClient.containers = List.of(container("container-1"));

        RemoveContainerPort.RemoveContainerResult result = adapter.remove(SERVICE_NAME);

        assertThat(result).isInstanceOf(RemoveContainerPort.RemoveContainerResult.Success.class);
        assertThat(dockerContainerClient.stoppedContainerIds).containsExactly("container-1");
        assertThat(dockerContainerClient.removedContainerIds).containsExactly("container-1");
    }

    @Test
    void returns_missing_when_no_managed_container_exists() {
        dockerContainerClient.containers = List.of();

        RemoveContainerPort.RemoveContainerResult result = adapter.remove(SERVICE_NAME);

        assertThat(result).isInstanceOf(RemoveContainerPort.RemoveContainerResult.MissingContainer.class);
        assertThat(dockerContainerClient.stoppedContainerIds).isEmpty();
        assertThat(dockerContainerClient.removedContainerIds).isEmpty();
    }

    @Test
    void returns_duplicate_when_multiple_managed_containers_exist() {
        dockerContainerClient.containers = List.of(container("container-1"), container("container-2"));

        RemoveContainerPort.RemoveContainerResult result = adapter.remove(SERVICE_NAME);

        assertThat(result).isInstanceOf(RemoveContainerPort.RemoveContainerResult.DuplicateManagedContainers.class);
        assertThat(dockerContainerClient.stoppedContainerIds).isEmpty();
        assertThat(dockerContainerClient.removedContainerIds).isEmpty();
    }

    @Test
    void returns_failure_when_listing_containers_fails() {
        dockerContainerClient.listFailure = new DockerException("docker unavailable", 500);

        RemoveContainerPort.RemoveContainerResult result = adapter.remove(SERVICE_NAME);

        assertThat(result).isInstanceOf(RemoveContainerPort.RemoveContainerResult.Failure.class);
    }

    @Test
    void returns_failure_when_stopping_container_fails() {
        dockerContainerClient.containers = List.of(container("container-1"));
        dockerContainerClient.stopFailure = new DockerException("docker unavailable", 500);

        RemoveContainerPort.RemoveContainerResult result = adapter.remove(SERVICE_NAME);

        assertThat(result).isInstanceOf(RemoveContainerPort.RemoveContainerResult.Failure.class);
        assertThat(dockerContainerClient.stoppedContainerIds).containsExactly("container-1");
        assertThat(dockerContainerClient.removedContainerIds).isEmpty();
    }

    @Test
    void returns_failure_when_removing_container_fails() {
        dockerContainerClient.containers = List.of(container("container-1"));
        dockerContainerClient.removeFailure = new DockerException("docker unavailable", 500);

        RemoveContainerPort.RemoveContainerResult result = adapter.remove(SERVICE_NAME);

        assertThat(result).isInstanceOf(RemoveContainerPort.RemoveContainerResult.Failure.class);
        assertThat(dockerContainerClient.stoppedContainerIds).containsExactly("container-1");
        assertThat(dockerContainerClient.removedContainerIds).containsExactly("container-1");
    }

    private static Container container(String id) {
        return new TestContainer(id);
    }

    private static final class FakeDockerContainerClient implements DockerContainerClient {

        private List<Container> containers = List.of();
        private final List<String> stoppedContainerIds = new ArrayList<>();
        private final List<String> removedContainerIds = new ArrayList<>();
        private DockerException listFailure;
        private DockerException stopFailure;
        private DockerException removeFailure;

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
        }

        @Override
        public void stopContainer(String containerId) {
            stoppedContainerIds.add(containerId);

            if (stopFailure != null) {
                throw stopFailure;
            }
        }

        @Override
        public void removeContainer(String containerId) {
            removedContainerIds.add(containerId);

            if (removeFailure != null) {
                throw removeFailure;
            }
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
