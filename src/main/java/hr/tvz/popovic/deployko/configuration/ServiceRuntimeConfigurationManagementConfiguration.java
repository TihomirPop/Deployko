package hr.tvz.popovic.deployko.configuration;

import hr.tvz.popovic.deployko.application.domain.service.ServiceRuntimeConfigurationDomainService;
import hr.tvz.popovic.deployko.application.port.in.CreateServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServicePortMappingsUseCase;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.FindServicePortMappingsPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceRuntimeConfigurationManagementConfiguration {

    @Bean
    ServiceRuntimeConfigurationDomainService serviceRuntimeConfigurationDomainService(
            FindServicePortMappingsPort findServicePortMappingsPort,
            CreateServicePortMappingPort createServicePortMappingPort,
            DeleteServicePortMappingPort deleteServicePortMappingPort
    ) {
        return new ServiceRuntimeConfigurationDomainService(
                findServicePortMappingsPort,
                createServicePortMappingPort,
                deleteServicePortMappingPort
        );
    }

    @Bean
    GetServicePortMappingsUseCase getServicePortMappingsUseCase(
            ServiceRuntimeConfigurationDomainService serviceRuntimeConfigurationDomainService
    ) {
        return serviceRuntimeConfigurationDomainService;
    }

    @Bean
    DeleteServicePortMappingUseCase deleteServicePortMappingUseCase(
            ServiceRuntimeConfigurationDomainService serviceRuntimeConfigurationDomainService
    ) {
        return serviceRuntimeConfigurationDomainService;
    }

    @Bean
    CreateServicePortMappingUseCase createServicePortMappingUseCase(
            ServiceRuntimeConfigurationDomainService serviceRuntimeConfigurationDomainService
    ) {
        return serviceRuntimeConfigurationDomainService;
    }

    @Bean
    @ConditionalOnMissingBean
    FindServicePortMappingsPort findServicePortMappingsPort() {
        return _ -> new FindServicePortMappingsPort.FindServicePortMappingsResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    CreateServicePortMappingPort createServicePortMappingPort() {
        return (_, _, _) -> new CreateServicePortMappingPort.CreateServicePortMappingResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    DeleteServicePortMappingPort deleteServicePortMappingPort() {
        return (_, _) -> new DeleteServicePortMappingPort.DeleteServicePortMappingResult.Failure();
    }
}
