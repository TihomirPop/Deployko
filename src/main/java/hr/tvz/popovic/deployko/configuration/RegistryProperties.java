package hr.tvz.popovic.deployko.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "deployko.registry")
public record RegistryProperties(DockerHub dockerHub) {

    public record DockerHub(String username, String password) {
    }
}
