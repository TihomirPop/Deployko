package hr.tvz.popovic.deployko.configuration;

import hr.tvz.popovic.deployko.application.domain.service.ServiceRuntimeConfigurationDomainService;
import hr.tvz.popovic.deployko.application.port.in.CreateServiceEnvironmentVariableUseCase;
import hr.tvz.popovic.deployko.application.port.in.CreateServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.CreateServiceVolumeMountUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServiceEnvironmentVariableUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServiceVolumeMountUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceEnvironmentVariablesUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServicePortMappingsUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceVolumeMountsUseCase;
import hr.tvz.popovic.deployko.application.port.in.UpdateServiceEnvironmentVariableUseCase;
import hr.tvz.popovic.deployko.application.port.in.UpdateServiceVolumeMountUseCase;
import hr.tvz.popovic.deployko.application.port.out.CreateServiceEnvironmentVariablePort;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.CreateServiceVolumeMountPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceEnvironmentVariablePort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceVolumeMountPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceEnvironmentVariablesPort;
import hr.tvz.popovic.deployko.application.port.out.FindServicePortMappingsPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceVolumeMountsPort;
import hr.tvz.popovic.deployko.application.port.out.UpdateServiceEnvironmentVariablePort;
import hr.tvz.popovic.deployko.application.port.out.UpdateServiceVolumeMountPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceRuntimeConfigurationManagementConfiguration {

    @Bean
    ServiceRuntimeConfigurationDomainService serviceRuntimeConfigurationDomainService(
            FindServiceEnvironmentVariablesPort findServiceEnvironmentVariablesPort,
            CreateServiceEnvironmentVariablePort createServiceEnvironmentVariablePort,
            UpdateServiceEnvironmentVariablePort updateServiceEnvironmentVariablePort,
            DeleteServiceEnvironmentVariablePort deleteServiceEnvironmentVariablePort,
            FindServicePortMappingsPort findServicePortMappingsPort,
            CreateServicePortMappingPort createServicePortMappingPort,
            DeleteServicePortMappingPort deleteServicePortMappingPort,
            FindServiceVolumeMountsPort findServiceVolumeMountsPort,
            CreateServiceVolumeMountPort createServiceVolumeMountPort,
            UpdateServiceVolumeMountPort updateServiceVolumeMountPort,
            DeleteServiceVolumeMountPort deleteServiceVolumeMountPort
    ) {
        return new ServiceRuntimeConfigurationDomainService(
                findServiceEnvironmentVariablesPort,
                createServiceEnvironmentVariablePort,
                updateServiceEnvironmentVariablePort,
                deleteServiceEnvironmentVariablePort,
                findServicePortMappingsPort,
                createServicePortMappingPort,
                deleteServicePortMappingPort,
                findServiceVolumeMountsPort,
                createServiceVolumeMountPort,
                updateServiceVolumeMountPort,
                deleteServiceVolumeMountPort
        );
    }

    @Bean
    CreateServiceEnvironmentVariableUseCase createServiceEnvironmentVariableUseCase(
            ServiceRuntimeConfigurationDomainService serviceRuntimeConfigurationDomainService
    ) {
        return serviceRuntimeConfigurationDomainService;
    }

    @Bean
    UpdateServiceEnvironmentVariableUseCase updateServiceEnvironmentVariableUseCase(
            ServiceRuntimeConfigurationDomainService serviceRuntimeConfigurationDomainService
    ) {
        return serviceRuntimeConfigurationDomainService;
    }

    @Bean
    DeleteServiceEnvironmentVariableUseCase deleteServiceEnvironmentVariableUseCase(
            ServiceRuntimeConfigurationDomainService serviceRuntimeConfigurationDomainService
    ) {
        return serviceRuntimeConfigurationDomainService;
    }

    @Bean
    GetServiceEnvironmentVariablesUseCase getServiceEnvironmentVariablesUseCase(
            ServiceRuntimeConfigurationDomainService serviceRuntimeConfigurationDomainService
    ) {
        return serviceRuntimeConfigurationDomainService;
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
    UpdateServiceVolumeMountUseCase updateServiceVolumeMountUseCase(
            ServiceRuntimeConfigurationDomainService serviceRuntimeConfigurationDomainService
    ) {
        return serviceRuntimeConfigurationDomainService;
    }

    @Bean
    DeleteServiceVolumeMountUseCase deleteServiceVolumeMountUseCase(
            ServiceRuntimeConfigurationDomainService serviceRuntimeConfigurationDomainService
    ) {
        return serviceRuntimeConfigurationDomainService;
    }

    @Bean
    @ConditionalOnMissingBean
    FindServiceEnvironmentVariablesPort findServiceEnvironmentVariablesPort() {
        return _ -> new FindServiceEnvironmentVariablesPort.FindServiceEnvironmentVariablesResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    CreateServiceEnvironmentVariablePort createServiceEnvironmentVariablePort() {
        return (_, _, _) -> new CreateServiceEnvironmentVariablePort.CreateServiceEnvironmentVariableResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    UpdateServiceEnvironmentVariablePort updateServiceEnvironmentVariablePort() {
        return (_, _, _) -> new UpdateServiceEnvironmentVariablePort.UpdateServiceEnvironmentVariableResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    DeleteServiceEnvironmentVariablePort deleteServiceEnvironmentVariablePort() {
        return (_, _) -> new DeleteServiceEnvironmentVariablePort.DeleteServiceEnvironmentVariableResult.Failure();
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

    @Bean
    @ConditionalOnMissingBean
    UpdateServiceVolumeMountPort updateServiceVolumeMountPort() {
        return (_, _) -> new UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    DeleteServiceVolumeMountPort deleteServiceVolumeMountPort() {
        return (_, _) -> new DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.Failure();
    }
}
