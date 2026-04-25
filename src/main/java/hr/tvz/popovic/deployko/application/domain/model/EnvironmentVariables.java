package hr.tvz.popovic.deployko.application.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public record EnvironmentVariables(List<EnvironmentVariable> entries) {

    public EnvironmentVariables {
        Objects.requireNonNull(entries, "entries must not be null");

        entries = List.copyOf(entries);
        ensureUniqueKeys(entries);
    }

    public boolean containsKey(String key) {
        Objects.requireNonNull(key, "key must not be null");

        return entries.stream()
                .anyMatch(entry -> entry.key().equals(key));
    }

    public Optional<EnvironmentVariable> getByKey(String key) {
        Objects.requireNonNull(key, "key must not be null");

        return entries.stream()
                .filter(entry -> entry.key().equals(key))
                .findFirst();
    }

    public EnvironmentVariables add(EnvironmentVariable environmentVariable) {
        Objects.requireNonNull(environmentVariable, "environmentVariable must not be null");

        if (containsKey(environmentVariable.key())) {
            throw new IllegalArgumentException("environment variable key must be unique");
        }

        return new EnvironmentVariables(withAppendedEntry(environmentVariable));
    }

    public EnvironmentVariables replace(EnvironmentVariable environmentVariable) {
        Objects.requireNonNull(environmentVariable, "environmentVariable must not be null");

        if (!containsKey(environmentVariable.key())) {
            throw new IllegalArgumentException("environment variable key must exist");
        }

        List<EnvironmentVariable> replacedEntries = entries.stream()
                .map(replaceMatchingKey(environmentVariable))
                .toList();

        return new EnvironmentVariables(replacedEntries);
    }

    public EnvironmentVariables removeByKey(String key) {
        Objects.requireNonNull(key, "key must not be null");

        List<EnvironmentVariable> remainingEntries = entries.stream()
                .filter(entry -> !entry.key().equals(key))
                .toList();

        return new EnvironmentVariables(remainingEntries);
    }

    public List<EnvironmentVariable> asList() {
        return entries;
    }

    private static void ensureUniqueKeys(List<EnvironmentVariable> entries) {
        long distinctKeys = entries.stream()
                .map(EnvironmentVariable::key)
                .distinct()
                .count();

        if (distinctKeys != entries.size()) {
            throw new IllegalArgumentException("environment variable keys must be unique");
        }
    }

    private List<EnvironmentVariable> withAppendedEntry(EnvironmentVariable environmentVariable) {
        return List.copyOf(
                entries.stream()
                        .collect(java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.toList(),
                                list -> {
                                    list.add(environmentVariable);
                                    return list;
                                }
                        ))
        );
    }

    private static Function<EnvironmentVariable, EnvironmentVariable> replaceMatchingKey(
            EnvironmentVariable replacement
    ) {
        return current -> current.key().equals(replacement.key()) ? replacement : current;
    }
}
