package hr.tvz.popovic.deployko.application.domain.model;

import java.util.Objects;

public record DesiredDeployment(
        ServiceName serviceName,
        ImageRepository imageRepository,
        ImageVersion imageVersion,
        RuntimeConfiguration runtimeConfiguration,
        DesiredDeploymentState desiredState
) {

    public DesiredDeployment {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(imageRepository, "imageRepository must not be null");
        Objects.requireNonNull(imageVersion, "imageVersion must not be null");
        Objects.requireNonNull(runtimeConfiguration, "runtimeConfiguration must not be null");
        Objects.requireNonNull(desiredState, "desiredState must not be null");
    }
}
