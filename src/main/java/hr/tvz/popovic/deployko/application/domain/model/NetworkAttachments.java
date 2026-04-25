package hr.tvz.popovic.deployko.application.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record NetworkAttachments(Map<NetworkAttachment.NetworkName, NetworkAttachment> entries) {

    public static NetworkAttachments empty() {
        return new NetworkAttachments(Map.of());
    }

    public NetworkAttachments {
        Objects.requireNonNull(entries, "entries must not be null");

        entries = copyAndValidate(entries);
    }

    public boolean containsNetworkName(NetworkAttachment.NetworkName networkName) {
        Objects.requireNonNull(networkName, "networkName must not be null");

        return entries.containsKey(networkName);
    }

    public Optional<NetworkAttachment> getByNetworkName(NetworkAttachment.NetworkName networkName) {
        Objects.requireNonNull(networkName, "networkName must not be null");

        return Optional.ofNullable(entries.get(networkName));
    }

    public NetworkAttachments add(NetworkAttachment networkAttachment) {
        Objects.requireNonNull(networkAttachment, "networkAttachment must not be null");

        if (entries.containsKey(networkAttachment.networkName())) {
            throw new IllegalArgumentException("network name must be unique");
        }

        Map<NetworkAttachment.NetworkName, NetworkAttachment> updatedEntries = new LinkedHashMap<>(entries);
        updatedEntries.put(networkAttachment.networkName(), networkAttachment);
        return new NetworkAttachments(updatedEntries);
    }

    public NetworkAttachments replace(NetworkAttachment networkAttachment) {
        Objects.requireNonNull(networkAttachment, "networkAttachment must not be null");

        if (!entries.containsKey(networkAttachment.networkName())) {
            throw new IllegalArgumentException("network name must exist");
        }

        Map<NetworkAttachment.NetworkName, NetworkAttachment> updatedEntries = new LinkedHashMap<>(entries);
        updatedEntries.put(networkAttachment.networkName(), networkAttachment);
        return new NetworkAttachments(updatedEntries);
    }

    public NetworkAttachments removeByNetworkName(NetworkAttachment.NetworkName networkName) {
        Objects.requireNonNull(networkName, "networkName must not be null");

        Map<NetworkAttachment.NetworkName, NetworkAttachment> updatedEntries = new LinkedHashMap<>(entries);
        updatedEntries.remove(networkName);
        return new NetworkAttachments(updatedEntries);
    }

    public Map<NetworkAttachment.NetworkName, NetworkAttachment> asMap() {
        return entries;
    }

    private static Map<NetworkAttachment.NetworkName, NetworkAttachment> copyAndValidate(
            Map<NetworkAttachment.NetworkName, NetworkAttachment> entries
    ) {
        LinkedHashMap<NetworkAttachment.NetworkName, NetworkAttachment> validatedEntries = new LinkedHashMap<>();

        for (Map.Entry<NetworkAttachment.NetworkName, NetworkAttachment> entry : entries.entrySet()) {
            NetworkAttachment.NetworkName networkName = Objects.requireNonNull(
                    entry.getKey(),
                    "networkName must not be null"
            );
            NetworkAttachment networkAttachment = Objects.requireNonNull(
                    entry.getValue(),
                    "networkAttachment must not be null"
            );

            if (!networkName.equals(networkAttachment.networkName())) {
                throw new IllegalArgumentException("network attachment map key must match attachment network name");
            }

            validatedEntries.put(networkName, networkAttachment);
        }

        return Collections.unmodifiableMap(validatedEntries);
    }
}
