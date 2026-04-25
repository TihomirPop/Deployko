package hr.tvz.popovic.deployko.application.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record DockerImage(String value) {

    private static final String NAME_COMPONENT = "[a-z0-9]+(?:[._-][a-z0-9]+)*";
    private static final String REPOSITORY_PATH = NAME_COMPONENT + "(?:/" + NAME_COMPONENT + ")*";
    private static final String REGISTRY = "(?:(?:[a-z0-9]+(?:[.-][a-z0-9]+)*)(?::[0-9]+)?/)?";
    private static final String TAG = "(?::[A-Za-z0-9_][A-Za-z0-9_.-]{0,127})?";
    private static final String DIGEST = "(?:@sha256:[a-f0-9]{64})?";
    private static final Pattern VALID_IMAGE_PATTERN = Pattern.compile("^" + REGISTRY + REPOSITORY_PATH + TAG + DIGEST + "$");

    public DockerImage {
        Objects.requireNonNull(value, "value must not be null");

        value = value.trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }

        if (!VALID_IMAGE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("value must be a valid docker image reference");
        }
    }
}
