package hr.tvz.popovic.deployko.configuration;

import hr.tvz.popovic.deployko.application.domain.service.DeploymentMonitor;
import hr.tvz.popovic.deployko.application.domain.service.DeploymentMonitorDomainService;
import hr.tvz.popovic.deployko.application.domain.service.ServiceRuntimeDomainService;
import hr.tvz.popovic.deployko.application.port.out.DeleteDesiredDeploymentPort;
import hr.tvz.popovic.deployko.application.port.out.DeployContainerPort;
import hr.tvz.popovic.deployko.application.port.out.FindActualDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.FindDesiredDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceDefinitionPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceSummaryCandidatesPort;
import hr.tvz.popovic.deployko.application.port.out.RecordDeploymentHistoryPort;
import hr.tvz.popovic.deployko.application.port.out.RemoveContainerPort;
import hr.tvz.popovic.deployko.application.port.out.ResolveDeploymentImagePort;
import hr.tvz.popovic.deployko.application.port.out.StartContainerPort;
import hr.tvz.popovic.deployko.application.port.out.StopContainerPort;
import hr.tvz.popovic.deployko.application.port.out.UpdateDeploymentStatusPort;
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
            RemoveContainerPort removeContainerPort,
            DeleteDesiredDeploymentPort deleteDesiredDeploymentPort,
            ResolveDeploymentImagePort resolveDeploymentImagePort,
            RecordDeploymentHistoryPort recordDeploymentHistoryPort,
            FindActualDeploymentStatePort findActualDeploymentStatePort,
            FindServiceSummaryCandidatesPort findServiceSummaryCandidatesPort,
            DeploymentMonitor deploymentMonitor
    ) {
        return new ServiceRuntimeDomainService(
                findServiceDefinitionPort,
                upsertDesiredDeploymentPort,
                updateDesiredDeploymentStatePort,
                findDesiredDeploymentStatePort,
                deployContainerPort,
                startContainerPort,
                stopContainerPort,
                removeContainerPort,
                deleteDesiredDeploymentPort,
                resolveDeploymentImagePort,
                recordDeploymentHistoryPort,
                findActualDeploymentStatePort,
                findServiceSummaryCandidatesPort,
                deploymentMonitor
        );
    }

    @Bean
    DeploymentMonitorDomainService deploymentMonitorDomainService(
            FindActualDeploymentStatePort findActualDeploymentStatePort,
            FindDesiredDeploymentStatePort findDesiredDeploymentStatePort,
            UpdateDeploymentStatusPort updateDeploymentStatusPort
    ) {
        return new DeploymentMonitorDomainService(
                findActualDeploymentStatePort,
                findDesiredDeploymentStatePort,
                updateDeploymentStatusPort
        );
    }
}
