package hr.tvz.popovic.deployko.application.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record PortMappings(Map<Port, Port> entries) {

    public static PortMappings empty() {
        return new PortMappings(Map.of());
    }

    public PortMappings {
        Objects.requireNonNull(entries, "entries must not be null");

        entries = copyAndValidate(entries);
    }

    public boolean containsHostPort(Port hostPort) {
        Objects.requireNonNull(hostPort, "hostPort must not be null");

        return entries.containsKey(hostPort);
    }

    public Optional<Port> getByHostPort(Port hostPort) {
        Objects.requireNonNull(hostPort, "hostPort must not be null");

        return Optional.ofNullable(entries.get(hostPort));
    }

    public PortMappings add(Port hostPort, Port containerPort) {
        Objects.requireNonNull(hostPort, "hostPort must not be null");
        Objects.requireNonNull(containerPort, "containerPort must not be null");

        if (entries.containsKey(hostPort)) {
            throw new IllegalArgumentException("host port must be unique");
        }

        if (entries.containsValue(containerPort)) {
            throw new IllegalArgumentException("container port must be unique");
        }

        Map<Port, Port> updatedEntries = new LinkedHashMap<>(entries);
        updatedEntries.put(hostPort, containerPort);
        return new PortMappings(updatedEntries);
    }

    public PortMappings replace(Port hostPort, Port containerPort) {
        Objects.requireNonNull(hostPort, "hostPort must not be null");
        Objects.requireNonNull(containerPort, "containerPort must not be null");

        if (!entries.containsKey(hostPort)) {
            throw new IllegalArgumentException("host port must exist");
        }

        Port existingContainerPort = entries.get(hostPort);
        if (!existingContainerPort.equals(containerPort) && entries.containsValue(containerPort)) {
            throw new IllegalArgumentException("container port must be unique");
        }

        Map<Port, Port> updatedEntries = new LinkedHashMap<>(entries);
        updatedEntries.put(hostPort, containerPort);
        return new PortMappings(updatedEntries);
    }

    public PortMappings removeByHostPort(Port hostPort) {
        Objects.requireNonNull(hostPort, "hostPort must not be null");

        Map<Port, Port> updatedEntries = new LinkedHashMap<>(entries);
        updatedEntries.remove(hostPort);
        return new PortMappings(updatedEntries);
    }

    public Map<Port, Port> asMap() {
        return entries;
    }

    private static Map<Port, Port> copyAndValidate(Map<Port, Port> entries) {
        LinkedHashMap<Port, Port> validatedEntries = new LinkedHashMap<>();

        for (Map.Entry<Port, Port> entry : entries.entrySet()) {
            Port hostPort = Objects.requireNonNull(entry.getKey(), "hostPort must not be null");
            Port containerPort = Objects.requireNonNull(entry.getValue(), "containerPort must not be null");

            if (validatedEntries.containsValue(containerPort)) {
                throw new IllegalArgumentException("container port must be unique");
            }

            validatedEntries.put(hostPort, containerPort);
        }

        return Collections.unmodifiableMap(validatedEntries);
    }
}
