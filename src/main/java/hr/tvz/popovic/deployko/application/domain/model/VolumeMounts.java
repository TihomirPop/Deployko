package hr.tvz.popovic.deployko.application.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record VolumeMounts(Map<VolumeMount.Target, VolumeMount> entries) {

    public static VolumeMounts empty() {
        return new VolumeMounts(Map.of());
    }

    public VolumeMounts {
        Objects.requireNonNull(entries, "entries must not be null");

        entries = copyAndValidate(entries);
    }

    public boolean containsTarget(VolumeMount.Target target) {
        Objects.requireNonNull(target, "target must not be null");

        return entries.containsKey(target);
    }

    public Optional<VolumeMount> getByTarget(VolumeMount.Target target) {
        Objects.requireNonNull(target, "target must not be null");

        return Optional.ofNullable(entries.get(target));
    }

    public VolumeMounts add(VolumeMount volumeMount) {
        Objects.requireNonNull(volumeMount, "volumeMount must not be null");

        if (entries.containsKey(volumeMount.target())) {
            throw new IllegalArgumentException("volume mount target must be unique");
        }

        Map<VolumeMount.Target, VolumeMount> updatedEntries = new LinkedHashMap<>(entries);
        updatedEntries.put(volumeMount.target(), volumeMount);
        return new VolumeMounts(updatedEntries);
    }

    public VolumeMounts replace(VolumeMount volumeMount) {
        Objects.requireNonNull(volumeMount, "volumeMount must not be null");

        if (!entries.containsKey(volumeMount.target())) {
            throw new IllegalArgumentException("volume mount target must exist");
        }

        Map<VolumeMount.Target, VolumeMount> updatedEntries = new LinkedHashMap<>(entries);
        updatedEntries.put(volumeMount.target(), volumeMount);
        return new VolumeMounts(updatedEntries);
    }

    public VolumeMounts removeByTarget(VolumeMount.Target target) {
        Objects.requireNonNull(target, "target must not be null");

        Map<VolumeMount.Target, VolumeMount> updatedEntries = new LinkedHashMap<>(entries);
        updatedEntries.remove(target);
        return new VolumeMounts(updatedEntries);
    }

    public Map<VolumeMount.Target, VolumeMount> asMap() {
        return entries;
    }

    private static Map<VolumeMount.Target, VolumeMount> copyAndValidate(
            Map<VolumeMount.Target, VolumeMount> entries
    ) {
        LinkedHashMap<VolumeMount.Target, VolumeMount> validatedEntries = new LinkedHashMap<>();

        for (Map.Entry<VolumeMount.Target, VolumeMount> entry : entries.entrySet()) {
            VolumeMount.Target target = Objects.requireNonNull(entry.getKey(), "target must not be null");
            VolumeMount volumeMount = Objects.requireNonNull(entry.getValue(), "volumeMount must not be null");

            if (!target.equals(volumeMount.target())) {
                throw new IllegalArgumentException("volume mount map key must match mount target");
            }

            validatedEntries.put(target, volumeMount);
        }

        return Collections.unmodifiableMap(validatedEntries);
    }
}
