package hr.tvz.popovic.deployko.application.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public sealed interface VolumeMount permits VolumeMount.BindMount, VolumeMount.NamedVolumeMount {

    Target target();

    boolean readOnly();

    record BindMount(HostPath source, Target target, boolean readOnly) implements VolumeMount {

        public BindMount {
            Objects.requireNonNull(source, "source must not be null");
            Objects.requireNonNull(target, "target must not be null");
        }
    }

    record NamedVolumeMount(VolumeName source, Target target, boolean readOnly) implements VolumeMount {

        public NamedVolumeMount {
            Objects.requireNonNull(source, "source must not be null");
            Objects.requireNonNull(target, "target must not be null");
        }
    }

    record HostPath(String value) {

        public HostPath {
            Objects.requireNonNull(value, "value must not be null");

            value = value.trim();

            if (value.isEmpty()) {
                throw new IllegalArgumentException("value must not be blank");
            }

            if (!value.startsWith("/")) {
                throw new IllegalArgumentException("value must be an absolute host path");
            }
        }
    }

    record Target(String value) {

        public Target {
            Objects.requireNonNull(value, "value must not be null");

            value = value.trim();

            if (value.isEmpty()) {
                throw new IllegalArgumentException("value must not be blank");
            }

            if (!value.startsWith("/")) {
                throw new IllegalArgumentException("value must be an absolute container path");
            }
        }
    }

    record VolumeName(String value) {

        private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_.-]*$");

        public VolumeName {
            Objects.requireNonNull(value, "value must not be null");

            value = value.trim();

            if (value.isEmpty()) {
                throw new IllegalArgumentException("value must not be blank");
            }

            if (!VALID_NAME_PATTERN.matcher(value).matches()) {
                throw new IllegalArgumentException("value must be a valid docker volume name");
            }
        }
    }
}
