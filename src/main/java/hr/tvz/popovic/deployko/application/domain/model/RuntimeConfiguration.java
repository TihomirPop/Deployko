package hr.tvz.popovic.deployko.application.domain.model;

import java.util.Objects;

public record RuntimeConfiguration(
        EnvironmentVariables environmentVariables,
        PortMappings portMappings,
        VolumeMounts volumeMounts,
        NetworkAttachments networkAttachments
) {

    public static RuntimeConfiguration empty() {
        return new RuntimeConfiguration(
                EnvironmentVariables.empty(),
                PortMappings.empty(),
                VolumeMounts.empty(),
                NetworkAttachments.empty()
        );
    }

    public RuntimeConfiguration {
        Objects.requireNonNull(environmentVariables, "environmentVariables must not be null");
        Objects.requireNonNull(portMappings, "portMappings must not be null");
        Objects.requireNonNull(volumeMounts, "volumeMounts must not be null");
        Objects.requireNonNull(networkAttachments, "networkAttachments must not be null");
    }
}
