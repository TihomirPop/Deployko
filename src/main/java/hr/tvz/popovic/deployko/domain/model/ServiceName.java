package hr.tvz.popovic.deployko.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record ServiceName(String value) {

    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9_.-]*$");

    public ServiceName {
        Objects.requireNonNull(value, "value must not be null");

        value = value.trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }

        if (!VALID_NAME_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("value must be docker-safe");
        }
    }
}
