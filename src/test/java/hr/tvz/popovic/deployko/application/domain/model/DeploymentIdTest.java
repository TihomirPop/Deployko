package hr.tvz.popovic.deployko.application.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DeploymentIdTest {

    @Test
    void accepts_uuid_value() {
        UUID value = UUID.fromString("018f4b5d-9c64-7000-9f2e-4d8fbf9f1b22");

        DeploymentId deploymentId = new DeploymentId(value);

        assertThat(deploymentId.value()).isEqualTo(value);
    }

    @Test
    void rejects_null_value() {
        assertThatNullPointerException()
                .isThrownBy(() -> new DeploymentId(null))
                .withMessage("value must not be null");
    }
}
