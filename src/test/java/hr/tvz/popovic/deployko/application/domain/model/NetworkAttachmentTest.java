package hr.tvz.popovic.deployko.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NetworkAttachmentTest {

    @Test
    void creates_network_attachment_when_values_are_valid() {
        NetworkAttachment networkAttachment = new NetworkAttachment(
                new NetworkAttachment.NetworkName("deployko_backend")
        );

        assertThat(networkAttachment.networkName())
                .isEqualTo(new NetworkAttachment.NetworkName("deployko_backend"));
    }

    @Test
    void trims_network_name_value() {
        NetworkAttachment.NetworkName networkName = new NetworkAttachment.NetworkName("  deployko_backend  ");

        assertThat(networkName.value()).isEqualTo("deployko_backend");
    }

    @Test
    void throws_when_network_name_is_null_on_attachment() {
        assertThatThrownBy(() -> new NetworkAttachment(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_network_name_value_is_null() {
        assertThatThrownBy(() -> new NetworkAttachment.NetworkName(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_network_name_value_is_blank() {
        assertThatThrownBy(() -> new NetworkAttachment.NetworkName("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_network_name_value_is_invalid() {
        assertThatThrownBy(() -> new NetworkAttachment.NetworkName("deployko/backend"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
