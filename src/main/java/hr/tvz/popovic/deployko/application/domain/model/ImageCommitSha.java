package hr.tvz.popovic.deployko.application.domain.model;

import java.util.Objects;

public sealed interface ImageCommitSha permits ImageCommitSha.Known, ImageCommitSha.Unknown {

    record Known(String value) implements ImageCommitSha {

        public Known {
            Objects.requireNonNull(value, "value must not be null");

            value = value.trim();

            if (value.isEmpty()) {
                throw new IllegalArgumentException("value must not be blank");
            }
        }
    }

    record Unknown() implements ImageCommitSha {
    }
}
