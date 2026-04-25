package hr.tvz.popovic.deployko.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NetworkAttachmentsTest {

    @Test
    void creates_network_attachments_when_entries_are_empty() {
        assertThat(NetworkAttachments.empty().entries()).isEmpty();
    }

    @Test
    void copies_entries_defensively() {
        NetworkAttachment.NetworkName firstNetworkName = new NetworkAttachment.NetworkName("deployko_backend");
        NetworkAttachment.NetworkName secondNetworkName = new NetworkAttachment.NetworkName("deployko_public");
        Map<NetworkAttachment.NetworkName, NetworkAttachment> entries = new LinkedHashMap<>();
        entries.put(firstNetworkName, new NetworkAttachment(firstNetworkName));

        NetworkAttachments networkAttachments = new NetworkAttachments(entries);
        entries.put(secondNetworkName, new NetworkAttachment(secondNetworkName));

        assertThat(networkAttachments.entries()).hasSize(1);
    }

    @Test
    void throws_when_entries_are_null() {
        assertThatThrownBy(() -> new NetworkAttachments(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_entries_contain_null_network_name() {
        Map<NetworkAttachment.NetworkName, NetworkAttachment> entries = new LinkedHashMap<>();
        entries.put(null, new NetworkAttachment(new NetworkAttachment.NetworkName("deployko_backend")));

        assertThatThrownBy(() -> new NetworkAttachments(entries))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_entries_contain_null_network_attachment() {
        Map<NetworkAttachment.NetworkName, NetworkAttachment> entries = new LinkedHashMap<>();
        entries.put(new NetworkAttachment.NetworkName("deployko_backend"), null);

        assertThatThrownBy(() -> new NetworkAttachments(entries))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throws_when_entry_key_does_not_match_attachment_network_name() {
        Map<NetworkAttachment.NetworkName, NetworkAttachment> entries = new LinkedHashMap<>();
        entries.put(
                new NetworkAttachment.NetworkName("deployko_backend"),
                new NetworkAttachment(new NetworkAttachment.NetworkName("deployko_public"))
        );

        assertThatThrownBy(() -> new NetworkAttachments(entries))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adds_network_attachment_when_network_name_is_unique() {
        NetworkAttachment networkAttachment = new NetworkAttachment(
                new NetworkAttachment.NetworkName("deployko_backend")
        );

        NetworkAttachments networkAttachments = NetworkAttachments.empty().add(networkAttachment);

        assertThat(networkAttachments.entries())
                .containsExactly(Map.entry(networkAttachment.networkName(), networkAttachment));
    }

    @Test
    void throws_when_adding_duplicate_network_name() {
        NetworkAttachment.NetworkName networkName = new NetworkAttachment.NetworkName("deployko_backend");
        NetworkAttachments networkAttachments = new NetworkAttachments(Map.of(
                networkName, new NetworkAttachment(networkName)
        ));

        assertThatThrownBy(() -> networkAttachments.add(new NetworkAttachment(networkName)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void replaces_network_attachment_for_existing_network_name() {
        NetworkAttachment.NetworkName networkName = new NetworkAttachment.NetworkName("deployko_backend");
        NetworkAttachment originalNetworkAttachment = new NetworkAttachment(networkName);
        NetworkAttachment replacementNetworkAttachment = new NetworkAttachment(networkName);
        NetworkAttachments networkAttachments = new NetworkAttachments(Map.of(networkName, originalNetworkAttachment));

        NetworkAttachments replacedNetworkAttachments = networkAttachments.replace(replacementNetworkAttachment);

        assertThat(replacedNetworkAttachments.getByNetworkName(networkName))
                .contains(replacementNetworkAttachment);
        assertThat(networkAttachments.getByNetworkName(networkName))
                .contains(originalNetworkAttachment);
    }

    @Test
    void throws_when_replacing_missing_network_name() {
        NetworkAttachment networkAttachment = new NetworkAttachment(
                new NetworkAttachment.NetworkName("deployko_backend")
        );

        assertThatThrownBy(() -> NetworkAttachments.empty().replace(networkAttachment))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removes_network_attachment_by_network_name() {
        NetworkAttachment.NetworkName firstNetworkName = new NetworkAttachment.NetworkName("deployko_backend");
        NetworkAttachment.NetworkName secondNetworkName = new NetworkAttachment.NetworkName("deployko_public");
        NetworkAttachment firstNetworkAttachment = new NetworkAttachment(firstNetworkName);
        NetworkAttachment secondNetworkAttachment = new NetworkAttachment(secondNetworkName);
        NetworkAttachments networkAttachments = new NetworkAttachments(Map.of(
                firstNetworkName, firstNetworkAttachment,
                secondNetworkName, secondNetworkAttachment
        ));

        NetworkAttachments remainingNetworkAttachments = networkAttachments.removeByNetworkName(firstNetworkName);

        assertThat(remainingNetworkAttachments.entries())
                .containsExactly(Map.entry(secondNetworkName, secondNetworkAttachment));
    }

    @Test
    void returns_network_attachment_by_network_name() {
        NetworkAttachment.NetworkName networkName = new NetworkAttachment.NetworkName("deployko_backend");
        NetworkAttachment networkAttachment = new NetworkAttachment(networkName);
        NetworkAttachments networkAttachments = new NetworkAttachments(Map.of(networkName, networkAttachment));

        assertThat(networkAttachments.getByNetworkName(networkName))
                .contains(networkAttachment);
    }

    @Test
    void returns_entries_as_map() {
        NetworkAttachment.NetworkName networkName = new NetworkAttachment.NetworkName("deployko_backend");
        NetworkAttachment networkAttachment = new NetworkAttachment(networkName);
        NetworkAttachments networkAttachments = new NetworkAttachments(Map.of(networkName, networkAttachment));

        assertThat(networkAttachments.asMap())
                .containsExactly(Map.entry(networkName, networkAttachment));
    }
}
