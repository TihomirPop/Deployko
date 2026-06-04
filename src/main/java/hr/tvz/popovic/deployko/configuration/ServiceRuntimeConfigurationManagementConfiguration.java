package hr.tvz.popovic.deployko.configuration;

import hr.tvz.popovic.deployko.application.domain.service.ServiceRuntimeConfigurationDomainService;
import hr.tvz.popovic.deployko.application.port.in.CreateServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.CreateServiceVolumeMountUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServicePortMappingsUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceVolumeMountsUseCase;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.CreateServiceVolumeMountPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.FindServicePortMappingsPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceVolumeMountsPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceRuntimeConfigurationManagementConfiguration {

    @Bean
    ServiceRuntimeConfigurationDomainService serviceRuntimeConfigurationDomainService(
            FindServicePortMappingsPort findServicePortMappingsPort,
            CreateServicePortMappingPort createServicePortMappingPort,
            DeleteServicePortMappingPort deleteServicePortMappingPort,
            FindServiceVolumeMountsPort findServiceVolumeMountsPort,
            CreateServiceVolumeMountPort createServiceVolumeMountPort
    ) {
        return new ServiceRuntimeConfigurationDomainService(
                findServicePortMappingsPort,
                createServicePortMappingPort,
                deleteServicePortMappingPort,
                findServiceVolumeMountsPort,
                createServiceVolumeMountPort
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
    GetServiceVolumeMountsUseCase getServiceVolumeMountsUseCase(
            ServiceRuntimeConfigurationDomainService serviceRuntimeConfigurationDomainService
    ) {
        return serviceRuntimeConfigurationDomainService;
    }

    @Bean
    CreateServiceVolumeMountUseCase createServiceVolumeMountUseCase(
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

    @Bean
    @ConditionalOnMissingBean
    FindServiceVolumeMountsPort findServiceVolumeMountsPort() {
        return _ -> new FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    CreateServiceVolumeMountPort createServiceVolumeMountPort() {
        return (_, _) -> new CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.Failure();
    }
}
