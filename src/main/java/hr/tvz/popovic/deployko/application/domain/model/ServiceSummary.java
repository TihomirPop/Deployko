package hr.tvz.popovic.deployko.application.domain.model;

import java.util.Objects;
import java.util.Optional;

public record ServiceSummary(
        ServiceName name,
        ImageRepository imageRepository,
        Optional<ImageVersion> deployedVersion,
        ServiceRuntimeStatus status
) {

    public ServiceSummary {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(imageRepository, "imageRepository must not be null");
        deployedVersion = Objects.requireNonNull(deployedVersion, "deployedVersion must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
