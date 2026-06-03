package hr.tvz.popovic.deployko.application.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record ImageVersion(String value) {

    private static final Pattern VALID_TAG_PATTERN = Pattern.compile("^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$");

    public ImageVersion {
        Objects.requireNonNull(value, "value must not be null");

        value = value.trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }

        if (!VALID_TAG_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("value must be a valid image tag");
        }
    }
}
