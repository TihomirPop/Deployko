package hr.tvz.popovic.deployko.configuration;

import hr.tvz.popovic.deployko.application.domain.service.ServiceRuntimeDomainService;
import hr.tvz.popovic.deployko.application.port.in.DeployServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceRuntimeStatusUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceSummariesUseCase;
import hr.tvz.popovic.deployko.application.port.in.StartServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.StopServiceUseCase;
import hr.tvz.popovic.deployko.application.port.out.DeployContainerPort;
import hr.tvz.popovic.deployko.application.port.out.FindActualDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.FindDesiredDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceDefinitionPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceSummaryCandidatesPort;
import hr.tvz.popovic.deployko.application.port.out.StartContainerPort;
import hr.tvz.popovic.deployko.application.port.out.StopContainerPort;
import hr.tvz.popovic.deployko.application.port.out.UpdateDesiredDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.UpsertDesiredDeploymentPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceRuntimeConfiguration {

    @Bean
    ServiceRuntimeDomainService serviceRuntimeDomainService(
            FindServiceDefinitionPort findServiceDefinitionPort,
            UpsertDesiredDeploymentPort upsertDesiredDeploymentPort,
            UpdateDesiredDeploymentStatePort updateDesiredDeploymentStatePort,
            FindDesiredDeploymentStatePort findDesiredDeploymentStatePort,
            DeployContainerPort deployContainerPort,
            StartContainerPort startContainerPort,
            StopContainerPort stopContainerPort,
            FindActualDeploymentStatePort findActualDeploymentStatePort,
            FindServiceSummaryCandidatesPort findServiceSummaryCandidatesPort
    ) {
        return new ServiceRuntimeDomainService(
                findServiceDefinitionPort,
                upsertDesiredDeploymentPort,
                updateDesiredDeploymentStatePort,
                findDesiredDeploymentStatePort,
                deployContainerPort,
                startContainerPort,
                stopContainerPort,
                findActualDeploymentStatePort,
                findServiceSummaryCandidatesPort
        );
    }

    @Bean
    DeployServiceUseCase deployServiceUseCase(ServiceRuntimeDomainService serviceRuntimeDomainService) {
        return serviceRuntimeDomainService;
    }

    @Bean
    StartServiceUseCase startServiceUseCase(ServiceRuntimeDomainService serviceRuntimeDomainService) {
        return serviceRuntimeDomainService;
    }

    @Bean
    StopServiceUseCase stopServiceUseCase(ServiceRuntimeDomainService serviceRuntimeDomainService) {
        return serviceRuntimeDomainService;
    }

    @Bean
    GetServiceRuntimeStatusUseCase getServiceRuntimeStatusUseCase(
            ServiceRuntimeDomainService serviceRuntimeDomainService
    ) {
        return serviceRuntimeDomainService;
    }

    @Bean
    GetServiceSummariesUseCase getServiceSummariesUseCase(ServiceRuntimeDomainService serviceRuntimeDomainService) {
        return serviceRuntimeDomainService;
    }
}
