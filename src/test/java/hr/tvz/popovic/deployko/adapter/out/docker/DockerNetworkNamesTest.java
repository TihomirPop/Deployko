package hr.tvz.popovic.deployko.adapter.out.docker;

import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachment;
import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachments;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DockerNetworkNamesTest {

    @Test
    void maps_network_attachments_to_docker_network_names() {
        NetworkAttachments networkAttachments = NetworkAttachments.empty()
                .add(new NetworkAttachment(new NetworkAttachment.NetworkName("backend")))
                .add(new NetworkAttachment(new NetworkAttachment.NetworkName("observability")));

        assertThat(DockerNetworkNames.from(networkAttachments))
                .containsExactly("backend", "observability");
    }

    @Test
    void maps_empty_network_attachments_to_empty_list() {
        assertThat(DockerNetworkNames.from(NetworkAttachments.empty()))
                .isEmpty();
    }
}
