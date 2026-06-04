package hr.tvz.popovic.deployko.configuration;

import hr.tvz.popovic.deployko.application.domain.service.ServiceDeploymentDomainService;
import hr.tvz.popovic.deployko.application.port.in.ServiceDeploymentUseCase;
import hr.tvz.popovic.deployko.application.port.out.DeployContainerPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceDefinitionPort;
import hr.tvz.popovic.deployko.application.port.out.StartContainerPort;
import hr.tvz.popovic.deployko.application.port.out.StopContainerPort;
import hr.tvz.popovic.deployko.application.port.out.UpdateDesiredDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.UpsertDesiredDeploymentPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceDeploymentConfiguration {

    @Bean
    ServiceDeploymentUseCase serviceDeploymentUseCase(
            FindServiceDefinitionPort findServiceDefinitionPort,
            UpsertDesiredDeploymentPort upsertDesiredDeploymentPort,
            UpdateDesiredDeploymentStatePort updateDesiredDeploymentStatePort,
            DeployContainerPort deployContainerPort,
            StartContainerPort startContainerPort,
            StopContainerPort stopContainerPort
    ) {
        return new ServiceDeploymentDomainService(
                findServiceDefinitionPort,
                upsertDesiredDeploymentPort,
                updateDesiredDeploymentStatePort,
                deployContainerPort,
                startContainerPort,
                stopContainerPort
        );
    }
}
