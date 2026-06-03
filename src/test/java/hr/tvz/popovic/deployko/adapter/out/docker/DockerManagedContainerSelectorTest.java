package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.model.Container;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DockerManagedContainerSelectorTest {

    @Test
    void builds_label_filter_for_service() {
        assertThat(DockerManagedContainerSelector.labelFilter(new ServiceName("deployko-api")))
                .containsEntry("deployko.managed", "true")
                .containsEntry("deployko.service.name", "deployko-api");
    }

    @Test
    void selects_found_when_exactly_one_container_is_present() {
        Container container = new Container();

        DockerManagedContainerSelector.ManagedContainerSelection selection =
                DockerManagedContainerSelector.selectSingle(List.of(container));

        assertThat(selection).isInstanceOf(DockerManagedContainerSelector.ManagedContainerSelection.Found.class);
        DockerManagedContainerSelector.ManagedContainerSelection.Found found =
                (DockerManagedContainerSelector.ManagedContainerSelection.Found) selection;
        assertThat(found.container()).isSameAs(container);
    }

    @Test
    void selects_missing_when_no_container_is_present() {
        DockerManagedContainerSelector.ManagedContainerSelection selection =
                DockerManagedContainerSelector.selectSingle(List.of());

        assertThat(selection).isInstanceOf(DockerManagedContainerSelector.ManagedContainerSelection.Missing.class);
    }

    @Test
    void selects_duplicate_when_more_than_one_container_is_present() {
        DockerManagedContainerSelector.ManagedContainerSelection selection =
                DockerManagedContainerSelector.selectSingle(List.of(new Container(), new Container()));

        assertThat(selection).isInstanceOf(DockerManagedContainerSelector.ManagedContainerSelection.Duplicate.class);
    }

    @Test
    void detects_managed_container_for_service() {
        Container container = new Container();
        container.labels = Map.of(
                "deployko.managed", "true",
                "deployko.service.name", "deployko-api"
        );

        boolean result = DockerManagedContainerSelector.isManagedForService(container, new ServiceName("deployko-api"));

        assertThat(result).isTrue();
    }

    @Test
    void rejects_container_for_different_service() {
        Container container = new Container();
        container.labels = Map.of(
                "deployko.managed", "true",
                "deployko.service.name", "other-api"
        );

        boolean result = DockerManagedContainerSelector.isManagedForService(container, new ServiceName("deployko-api"));

        assertThat(result).isFalse();
    }
}
