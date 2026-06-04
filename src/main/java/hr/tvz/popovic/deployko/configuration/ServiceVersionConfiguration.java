package hr.tvz.popovic.deployko.configuration;

import hr.tvz.popovic.deployko.adapter.out.registry.RegistryFindImageVersionsAdapter;
import hr.tvz.popovic.deployko.application.domain.service.ServiceVersionDomainService;
import hr.tvz.popovic.deployko.application.port.in.GetServiceVersionsUseCase;
import hr.tvz.popovic.deployko.application.port.out.FindImageVersionsPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceDefinitionPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceVersionConfiguration {

    @Bean
    ServiceVersionDomainService serviceVersionDomainService(
            FindServiceDefinitionPort findServiceDefinitionPort,
            FindImageVersionsPort findImageVersionsPort
    ) {
        return new ServiceVersionDomainService(findServiceDefinitionPort, findImageVersionsPort);
    }

    @Bean
    GetServiceVersionsUseCase getServiceVersionsUseCase(ServiceVersionDomainService serviceVersionDomainService) {
        return serviceVersionDomainService;
    }

    @Bean
    FindImageVersionsPort findImageVersionsPort() {
        return new RegistryFindImageVersionsAdapter();
    }
}
