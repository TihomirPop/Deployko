package hr.tvz.popovic.deployko.configuration;

import hr.tvz.popovic.deployko.application.domain.service.ServiceManagementDomainService;
import hr.tvz.popovic.deployko.application.port.in.ServiceManagementUseCase;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceByNamePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceManagementConfiguration {

    @Bean
    ServiceManagementUseCase serviceManagementUseCase(
            CreateServicePort createServicePort,
            DeleteServiceByNamePort deleteServiceByNamePort
    ) {
        return new ServiceManagementDomainService(createServicePort, deleteServiceByNamePort);
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
