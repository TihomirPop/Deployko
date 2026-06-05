package hr.tvz.popovic.deployko.configuration;

import hr.tvz.popovic.deployko.application.domain.service.ServiceDefinitionDomainService;
import hr.tvz.popovic.deployko.application.port.in.CreateServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServiceUseCase;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceByNamePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceDefinitionConfiguration {

    @Bean
    ServiceDefinitionDomainService serviceDefinitionDomainService(
            CreateServicePort createServicePort,
            DeleteServiceByNamePort deleteServiceByNamePort
    ) {
        return new ServiceDefinitionDomainService(createServicePort, deleteServiceByNamePort);
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
    @ConditionalOnMissingBean
    CreateServicePort createServicePort() {
        return _ -> new CreateServicePort.CreateServicePortResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    DeleteServiceByNamePort deleteServiceByNamePort() {
        return _ -> new DeleteServiceByNamePort.DeleteServiceByNameResult.Failure();
    }
}
