package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Container;
import hr.tvz.popovic.deployko.application.domain.model.ActualDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.FindActualDeploymentStatePort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DockerFindActualDeploymentStateAdapterTest {

    private static final ServiceName SERVICE_NAME = new ServiceName("deployko-api");

    private final FakeDockerContainerClient dockerContainerClient = new FakeDockerContainerClient();
    private final DockerFindActualDeploymentStateAdapter adapter =
            new DockerFindActualDeploymentStateAdapter(dockerContainerClient);

    @Test
    void returns_running_when_single_managed_container_is_running() {
        dockerContainerClient.containers = List.of(container("container-1", "running"));
        dockerContainerClient.restartCounts = java.util.Map.of("container-1", 3);

        FindActualDeploymentStatePort.FindActualDeploymentStateResult result = adapter.findActualState(SERVICE_NAME);

        assertThat(result).isEqualTo(new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found(
                ActualDeploymentState.RUNNING,
                3
        ));
    }

    @Test
    void returns_stopped_when_single_managed_container_is_not_running() {
        dockerContainerClient.containers = List.of(container("container-1", "exited"));

        FindActualDeploymentStatePort.FindActualDeploymentStateResult result = adapter.findActualState(SERVICE_NAME);

        assertThat(result).isEqualTo(new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found(
                ActualDeploymentState.STOPPED
        ));
    }

    @Test
    void returns_missing_when_no_managed_container_exists() {
        dockerContainerClient.containers = List.of();

        FindActualDeploymentStatePort.FindActualDeploymentStateResult result = adapter.findActualState(SERVICE_NAME);

        assertThat(result).isEqualTo(new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found(
                ActualDeploymentState.MISSING
        ));
    }

    @Test
    void returns_duplicate_when_multiple_managed_containers_exist() {
        dockerContainerClient.containers = List.of(
                container("container-1", "running"),
                container("container-2", "exited")
        );

        FindActualDeploymentStatePort.FindActualDeploymentStateResult result = adapter.findActualState(SERVICE_NAME);

        assertThat(result)
                .isInstanceOf(
                        FindActualDeploymentStatePort.FindActualDeploymentStateResult.DuplicateManagedContainers.class
                );
    }

    @Test
    void returns_failure_when_listing_containers_fails() {
        dockerContainerClient.listFailure = new DockerException("docker unavailable", 500);

        FindActualDeploymentStatePort.FindActualDeploymentStateResult result = adapter.findActualState(SERVICE_NAME);

        assertThat(result)
                .isInstanceOf(FindActualDeploymentStatePort.FindActualDeploymentStateResult.Failure.class);
    }

    private static Container container(String id, String state) {
        return new TestContainer(id, state);
    }

    private static final class FakeDockerContainerClient implements DockerContainerClient {

        private List<Container> containers = List.of();
        private java.util.Map<String, Integer> restartCounts = java.util.Map.of();
        private DockerException listFailure;

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
            return restartCounts.getOrDefault(containerId, 0);
        }

        @Override
        public void startContainer(String containerId) {
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
        private final String state;

        private TestContainer(String id, String state) {
            this.id = id;
            this.state = state;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getState() {
            return state;
        }
    }
}
