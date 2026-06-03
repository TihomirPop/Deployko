package hr.tvz.popovic.deployko.configuration;

import hr.tvz.popovic.deployko.application.domain.service.ServiceDeploymentDomainService;
import hr.tvz.popovic.deployko.application.port.in.ServiceDeploymentUseCase;
import hr.tvz.popovic.deployko.application.port.out.DeployContainerPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceDefinitionPort;
import hr.tvz.popovic.deployko.application.port.out.UpsertDesiredDeploymentPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceDeploymentConfiguration {

    @Bean
    ServiceDeploymentUseCase serviceDeploymentUseCase(
            FindServiceDefinitionPort findServiceDefinitionPort,
            UpsertDesiredDeploymentPort upsertDesiredDeploymentPort,
            DeployContainerPort deployContainerPort
    ) {
        return new ServiceDeploymentDomainService(
                findServiceDefinitionPort,
                upsertDesiredDeploymentPort,
                deployContainerPort
        );
    }
}
