package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.model.Container;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

import java.util.List;
import java.util.Map;
import java.util.Objects;

final class DockerManagedContainerSelector {

    private DockerManagedContainerSelector() {
    }

    static Map<String, String> labelFilter(ServiceName serviceName) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");

        return Map.of(
                DockerDeploymentMetadata.MANAGED_LABEL,
                DockerDeploymentMetadata.MANAGED_LABEL_VALUE,
                DockerDeploymentMetadata.SERVICE_NAME_LABEL,
                serviceName.value()
        );
    }

    static ManagedContainerSelection selectSingle(List<Container> containers) {
        Objects.requireNonNull(containers, "containers must not be null");

        return switch (containers.size()) {
            case 0 -> new ManagedContainerSelection.Missing();
            case 1 -> new ManagedContainerSelection.Found(containers.getFirst());
            default -> new ManagedContainerSelection.Duplicate();
        };
    }

    static boolean isManagedForService(Container container, ServiceName serviceName) {
        Objects.requireNonNull(container, "container must not be null");
        Objects.requireNonNull(serviceName, "serviceName must not be null");

        Map<String, String> labels = container.getLabels();
        if (labels == null) {
            return false;
        }

        return DockerDeploymentMetadata.MANAGED_LABEL_VALUE.equals(labels.get(DockerDeploymentMetadata.MANAGED_LABEL))
                && serviceName.value().equals(labels.get(DockerDeploymentMetadata.SERVICE_NAME_LABEL));
    }

    sealed interface ManagedContainerSelection
            permits ManagedContainerSelection.Found, ManagedContainerSelection.Missing, ManagedContainerSelection.Duplicate {

        record Found(Container container) implements ManagedContainerSelection {
        }

        record Missing() implements ManagedContainerSelection {
        }

        record Duplicate() implements ManagedContainerSelection {
        }
    }
}
