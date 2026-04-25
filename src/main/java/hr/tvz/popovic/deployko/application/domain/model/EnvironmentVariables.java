package hr.tvz.popovic.deployko.application.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public record EnvironmentVariables(Map<EnvironmentVariables.Key, EnvironmentVariables.Value> entries) {

    public static EnvironmentVariables empty() {
        return new EnvironmentVariables(Map.of());
    }

    public EnvironmentVariables {
        Objects.requireNonNull(entries, "entries must not be null");

        entries = copyAndValidate(entries);
    }

    public boolean containsKey(Key key) {
        Objects.requireNonNull(key, "key must not be null");

        return entries.containsKey(key);
    }

    public Optional<Value> getByKey(Key key) {
        Objects.requireNonNull(key, "key must not be null");

        return Optional.ofNullable(entries.get(key));
    }

    public EnvironmentVariables add(Key key, Value value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");

        if (entries.containsKey(key)) {
            throw new IllegalArgumentException("environment variable key must be unique");
        }

        Map<Key, Value> updatedEntries = new LinkedHashMap<>(entries);
        updatedEntries.put(key, value);
        return new EnvironmentVariables(updatedEntries);
    }

    public EnvironmentVariables replace(Key key, Value value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");

        if (!entries.containsKey(key)) {
            throw new IllegalArgumentException("environment variable key must exist");
        }

        Map<Key, Value> updatedEntries = new LinkedHashMap<>(entries);
        updatedEntries.put(key, value);
        return new EnvironmentVariables(updatedEntries);
    }

    public EnvironmentVariables removeByKey(Key key) {
        Objects.requireNonNull(key, "key must not be null");

        Map<Key, Value> updatedEntries = new LinkedHashMap<>(entries);
        updatedEntries.remove(key);
        return new EnvironmentVariables(updatedEntries);
    }

    public Map<Key, Value> asMap() {
        return entries;
    }

    private static Map<Key, Value> copyAndValidate(Map<Key, Value> entries) {
        LinkedHashMap<Key, Value> validatedEntries = new LinkedHashMap<>();

        for (Map.Entry<Key, Value> entry : entries.entrySet()) {
            Key key = Objects.requireNonNull(entry.getKey(), "key must not be null");
            Value value = Objects.requireNonNull(entry.getValue(), "value must not be null");
            validatedEntries.put(key, value);
        }

        return Collections.unmodifiableMap(validatedEntries);
    }

    public record Key(String value) {

        private static final Pattern VALID_KEY_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

        public Key {
            Objects.requireNonNull(value, "value must not be null");

            value = value.trim();

            if (value.isEmpty()) {
                throw new IllegalArgumentException("value must not be blank");
            }

            if (!VALID_KEY_PATTERN.matcher(value).matches()) {
                throw new IllegalArgumentException("value must be a valid environment variable name");
            }
        }
    }

    public record Value(String value) {

        public Value {
            Objects.requireNonNull(value, "value must not be null");
        }
    }
}
