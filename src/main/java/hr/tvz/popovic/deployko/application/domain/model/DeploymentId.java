package hr.tvz.popovic.deployko.application.domain.model;

import java.util.Objects;
import java.util.UUID;

public record DeploymentId(UUID value) {

    public DeploymentId {
        Objects.requireNonNull(value, "value must not be null");
    }
}
