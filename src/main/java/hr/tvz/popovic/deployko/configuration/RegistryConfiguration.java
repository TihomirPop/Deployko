package hr.tvz.popovic.deployko.configuration;

import io.github.ya_b.registry.client.RegistryClient;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(RegistryProperties.class)
public class RegistryConfiguration {

    @Bean
    ApplicationRunner dockerHubRegistryAuthentication(RegistryProperties registryProperties) {
        return _ -> {
            RegistryProperties.DockerHub dockerHub = registryProperties.dockerHub();
            if (dockerHub == null || !StringUtils.hasText(dockerHub.username()) || !StringUtils.hasText(dockerHub.password())) {
                throw new IllegalStateException("dockerHub registry credentials must be provided");
            }

            RegistryClient.authDockerHub(dockerHub.username(), dockerHub.password());
        };
    }
}
