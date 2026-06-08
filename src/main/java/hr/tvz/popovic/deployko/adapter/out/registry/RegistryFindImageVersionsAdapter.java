package hr.tvz.popovic.deployko.adapter.out.registry;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.port.out.FindImageVersionsPort;
import io.github.ya_b.registry.client.RegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

public final class RegistryFindImageVersionsAdapter implements FindImageVersionsPort {

    private static final Logger log = LoggerFactory.getLogger(RegistryFindImageVersionsAdapter.class);

    @Override
    public FindImageVersionsResult findImageVersions(ImageRepository imageRepository) {
        Objects.requireNonNull(imageRepository, "imageRepository must not be null");

        try {
            return new FindImageVersionsResult.Found(RegistryClient.tags(imageRepository.value())
                    .stream()
                    .map(ImageVersion::new)
                    .toList());
        } catch (IOException | IllegalArgumentException | NullPointerException exception) {
            log.error("error while finding image versions for repository {}", imageRepository.value(), exception);
            return new FindImageVersionsResult.Failure();
        }
    }
}
