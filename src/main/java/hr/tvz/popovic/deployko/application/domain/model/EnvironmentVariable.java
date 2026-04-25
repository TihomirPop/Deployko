package hr.tvz.popovic.deployko.application.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record EnvironmentVariable(String key, String value) {

    private static final Pattern VALID_KEY_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    public EnvironmentVariable {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");

        key = key.trim();

        if (key.isEmpty()) {
            throw new IllegalArgumentException("key must not be blank");
        }

        if (!VALID_KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("key must be a valid environment variable name");
        }
    }
}
