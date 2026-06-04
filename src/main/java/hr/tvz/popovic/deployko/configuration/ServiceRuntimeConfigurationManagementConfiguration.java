package hr.tvz.popovic.deployko.configuration;

import hr.tvz.popovic.deployko.application.domain.service.ServiceRuntimeConfigurationDomainService;
import hr.tvz.popovic.deployko.application.port.in.GetServicePortMappingsUseCase;
import hr.tvz.popovic.deployko.application.port.out.FindServicePortMappingsPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceRuntimeConfigurationManagementConfiguration {

    @Bean
    ServiceRuntimeConfigurationDomainService serviceRuntimeConfigurationDomainService(
            FindServicePortMappingsPort findServicePortMappingsPort
    ) {
        return new ServiceRuntimeConfigurationDomainService(findServicePortMappingsPort);
    }

    @Bean
    GetServicePortMappingsUseCase getServicePortMappingsUseCase(
            ServiceRuntimeConfigurationDomainService serviceRuntimeConfigurationDomainService
    ) {
        return serviceRuntimeConfigurationDomainService;
    }

    @Bean
    @ConditionalOnMissingBean
    FindServicePortMappingsPort findServicePortMappingsPort() {
        return _ -> new FindServicePortMappingsPort.FindServicePortMappingsResult.Failure();
    }
}
