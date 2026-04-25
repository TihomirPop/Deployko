package hr.tvz.popovic.deployko.application.domain.model;

import java.util.Objects;

public record Service(
        ServiceName name,
        ImageRepository imageRepository,
        RuntimeConfiguration runtimeConfiguration
) {

    public Service {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(imageRepository, "imageRepository must not be null");
        Objects.requireNonNull(runtimeConfiguration, "runtimeConfiguration must not be null");
    }
}
