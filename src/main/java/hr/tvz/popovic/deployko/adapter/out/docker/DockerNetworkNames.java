package hr.tvz.popovic.deployko.adapter.out.docker;

import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachment;
import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachments;
import java.util.List;
import java.util.Objects;

final class DockerNetworkNames {

    private DockerNetworkNames() {
    }

    static List<String> from(NetworkAttachments networkAttachments) {
        Objects.requireNonNull(networkAttachments, "networkAttachments must not be null");

        return networkAttachments.asMap().values().stream()
                .map(NetworkAttachment::networkName)
                .map(NetworkAttachment.NetworkName::value)
                .toList();
    }
}
