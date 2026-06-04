package hr.tvz.popovic.deployko.configuration;

import hr.tvz.popovic.deployko.application.domain.service.ServiceDefinitionDomainService;
import hr.tvz.popovic.deployko.application.port.in.CreateServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceNamesUseCase;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceByNamePort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceNamesPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceDefinitionConfiguration {

    @Bean
    ServiceDefinitionDomainService serviceDefinitionDomainService(
            CreateServicePort createServicePort,
            DeleteServiceByNamePort deleteServiceByNamePort,
            FindServiceNamesPort findServiceNamesPort
    ) {
        return new ServiceDefinitionDomainService(createServicePort, deleteServiceByNamePort, findServiceNamesPort);
    }

    @Bean
    CreateServiceUseCase createServiceUseCase(ServiceDefinitionDomainService serviceDefinitionDomainService) {
        return serviceDefinitionDomainService;
    }

    @Bean
    DeleteServiceUseCase deleteServiceUseCase(ServiceDefinitionDomainService serviceDefinitionDomainService) {
        return serviceDefinitionDomainService;
    }

    @Bean
    GetServiceNamesUseCase getServiceNamesUseCase(ServiceDefinitionDomainService serviceDefinitionDomainService) {
        return serviceDefinitionDomainService;
    }

    @Bean
    @ConditionalOnMissingBean
    CreateServicePort createServicePort() {
        return _ -> new CreateServicePort.CreateServicePortResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    DeleteServiceByNamePort deleteServiceByNamePort() {
        return _ -> new DeleteServiceByNamePort.DeleteServiceByNameResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    FindServiceNamesPort findServiceNamesPort() {
        return () -> new FindServiceNamesPort.FindServiceNamesResult.Failure();
    }
}
