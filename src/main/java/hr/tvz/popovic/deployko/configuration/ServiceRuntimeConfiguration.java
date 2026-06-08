package hr.tvz.popovic.deployko.configuration;

import hr.tvz.popovic.deployko.application.domain.service.DeploymentMonitor;
import hr.tvz.popovic.deployko.application.domain.service.DeploymentMonitorDomainService;
import hr.tvz.popovic.deployko.application.domain.service.RuntimeDeploymentDomainService;
import hr.tvz.popovic.deployko.application.domain.service.RuntimeLifecycleDomainService;
import hr.tvz.popovic.deployko.application.domain.service.RuntimeStatusDomainService;
import hr.tvz.popovic.deployko.application.domain.service.ServiceRuntimeDomainService;
import hr.tvz.popovic.deployko.application.domain.service.ServiceSummaryDomainService;
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
    RuntimeDeploymentDomainService runtimeDeploymentDomainService(
            FindServiceDefinitionPort findServiceDefinitionPort,
            UpsertDesiredDeploymentPort upsertDesiredDeploymentPort,
            DeployContainerPort deployContainerPort,
            ResolveDeploymentImagePort resolveDeploymentImagePort,
            RecordDeploymentHistoryPort recordDeploymentHistoryPort,
            UpdateDeploymentStatusPort updateDeploymentStatusPort,
            DeploymentMonitor deploymentMonitor
    ) {
        return new RuntimeDeploymentDomainService(
                findServiceDefinitionPort,
                resolveDeploymentImagePort,
                recordDeploymentHistoryPort,
                upsertDesiredDeploymentPort,
                deployContainerPort,
                updateDeploymentStatusPort,
                deploymentMonitor
        );
    }

    @Bean
    RuntimeLifecycleDomainService runtimeLifecycleDomainService(
            UpdateDesiredDeploymentStatePort updateDesiredDeploymentStatePort,
            FindDesiredDeploymentStatePort findDesiredDeploymentStatePort,
            StartContainerPort startContainerPort,
            StopContainerPort stopContainerPort,
            RemoveContainerPort removeContainerPort,
            DeleteDesiredDeploymentPort deleteDesiredDeploymentPort
    ) {
        return new RuntimeLifecycleDomainService(
                updateDesiredDeploymentStatePort,
                findDesiredDeploymentStatePort,
                startContainerPort,
                stopContainerPort,
                removeContainerPort,
                deleteDesiredDeploymentPort
        );
    }

    @Bean
    RuntimeStatusDomainService runtimeStatusDomainService(
            FindDesiredDeploymentStatePort findDesiredDeploymentStatePort,
            FindActualDeploymentStatePort findActualDeploymentStatePort
    ) {
        return new RuntimeStatusDomainService(findDesiredDeploymentStatePort, findActualDeploymentStatePort);
    }

    @Bean
    ServiceSummaryDomainService serviceSummaryDomainService(
            FindServiceSummaryCandidatesPort findServiceSummaryCandidatesPort,
            RuntimeStatusDomainService runtimeStatusDomainService
    ) {
        return new ServiceSummaryDomainService(findServiceSummaryCandidatesPort, runtimeStatusDomainService);
    }

    @Bean
    ServiceRuntimeDomainService serviceRuntimeDomainService(
            RuntimeDeploymentDomainService runtimeDeploymentDomainService,
            RuntimeLifecycleDomainService runtimeLifecycleDomainService,
            RuntimeStatusDomainService runtimeStatusDomainService,
            ServiceSummaryDomainService serviceSummaryDomainService
    ) {
        return new ServiceRuntimeDomainService(
                runtimeDeploymentDomainService,
                runtimeLifecycleDomainService,
                runtimeStatusDomainService,
                serviceSummaryDomainService
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
