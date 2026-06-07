package hr.tvz.popovic.deployko.configuration;

import hr.tvz.popovic.deployko.application.domain.service.DeploymentHistoryDomainService;
import hr.tvz.popovic.deployko.application.port.out.FindLatestDeploymentPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeploymentHistoryConfiguration {

    @Bean
    DeploymentHistoryDomainService deploymentHistoryDomainService(FindLatestDeploymentPort findLatestDeploymentPort) {
        return new DeploymentHistoryDomainService(findLatestDeploymentPort);
    }
}
