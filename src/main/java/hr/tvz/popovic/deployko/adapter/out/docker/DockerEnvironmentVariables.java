package hr.tvz.popovic.deployko.adapter.out.docker;

import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import java.util.List;
import java.util.Objects;

final class DockerEnvironmentVariables {

    private DockerEnvironmentVariables() {
    }

    static List<String> from(EnvironmentVariables environmentVariables) {
        Objects.requireNonNull(environmentVariables, "environmentVariables must not be null");

        return environmentVariables.asMap().entrySet().stream()
                .map(entry -> entry.getKey().value() + "=" + entry.getValue().value())
                .toList();
    }
}
