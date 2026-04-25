package hr.tvz.popovic.deployko.application.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record NetworkAttachment(NetworkName networkName) {

    public NetworkAttachment {
        Objects.requireNonNull(networkName, "networkName must not be null");
    }

    public record NetworkName(String value) {

        private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_.-]*$");

        public NetworkName {
            Objects.requireNonNull(value, "value must not be null");

            value = value.trim();

            if (value.isEmpty()) {
                throw new IllegalArgumentException("value must not be blank");
            }

            if (!VALID_NAME_PATTERN.matcher(value).matches()) {
                throw new IllegalArgumentException("value must be a valid docker network name");
            }
        }
    }
}
