package hr.tvz.popovic.deployko.adapter.out.registry;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.port.out.FindImageVersionsPort;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegistryFindImageVersionsAdapterTest {

    @Test
    void maps_registry_tags_to_image_versions() {
        ImageRepository imageRepository = new ImageRepository("ghcr.io/deployko/api");
        FakeRegistryImageVersionClient registryClient = new FakeRegistryImageVersionClient(List.of(
                "1.0.0",
                "latest"
        ));
        RegistryFindImageVersionsAdapter adapter = new RegistryFindImageVersionsAdapter(registryClient);

        FindImageVersionsPort.FindImageVersionsResult result = adapter.findImageVersions(imageRepository);

        assertThat(result).isEqualTo(new FindImageVersionsPort.FindImageVersionsResult.Found(List.of(
                new ImageVersion("1.0.0"),
                new ImageVersion("latest")
        )));
        assertThat(registryClient.imageRepository).isEqualTo("ghcr.io/deployko/api");
    }

    @Test
    void returns_failure_when_registry_client_fails() {
        RegistryFindImageVersionsAdapter adapter = new RegistryFindImageVersionsAdapter(_ -> {
            throw new IOException("registry unavailable");
        });

        FindImageVersionsPort.FindImageVersionsResult result = adapter.findImageVersions(
                new ImageRepository("ghcr.io/deployko/api")
        );

        assertThat(result).isInstanceOf(FindImageVersionsPort.FindImageVersionsResult.Failure.class);
    }

    @Test
    void returns_failure_when_registry_returns_invalid_tag() {
        RegistryFindImageVersionsAdapter adapter = new RegistryFindImageVersionsAdapter(_ -> List.of("feature/build"));

        FindImageVersionsPort.FindImageVersionsResult result = adapter.findImageVersions(
                new ImageRepository("ghcr.io/deployko/api")
        );

        assertThat(result).isInstanceOf(FindImageVersionsPort.FindImageVersionsResult.Failure.class);
    }

    private static final class FakeRegistryImageVersionClient implements RegistryImageVersionClient {

        private final List<String> tags;
        private String imageRepository;

        private FakeRegistryImageVersionClient(List<String> tags) {
            this.tags = tags;
        }

        @Override
        public List<String> tags(String imageRepository) {
            this.imageRepository = imageRepository;
            return tags;
        }
    }
}
