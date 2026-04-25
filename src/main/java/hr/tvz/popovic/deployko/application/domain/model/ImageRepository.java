package hr.tvz.popovic.deployko.application.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record ImageRepository(String value) {

    private static final String NAME_COMPONENT = "[a-z0-9]+(?:[._-][a-z0-9]+)*";
    private static final String REPOSITORY_PATH = NAME_COMPONENT + "(?:/" + NAME_COMPONENT + ")*";
    private static final String REGISTRY = "(?:(?:[a-z0-9]+(?:[.-][a-z0-9]+)*)(?::[0-9]+)?/)?";
    private static final Pattern VALID_REPOSITORY_PATTERN = Pattern.compile("^" + REGISTRY + REPOSITORY_PATH + "$");

    public ImageRepository {
        Objects.requireNonNull(value, "value must not be null");

        value = value.trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }

        if (!VALID_REPOSITORY_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("value must be a valid image repository");
        }
    }
}
