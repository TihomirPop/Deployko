package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.port.in.DeployServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceRuntimeStatusUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceSummariesUseCase;
import hr.tvz.popovic.deployko.application.port.in.StartServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.StopServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.UninstallServiceUseCase;
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

import java.util.Objects;

public final class ServiceRuntimeDomainService
        implements DeployServiceUseCase, StartServiceUseCase, StopServiceUseCase, GetServiceRuntimeStatusUseCase,
        GetServiceSummariesUseCase, UninstallServiceUseCase {

    private final RuntimeDeploymentDomainService runtimeDeploymentDomainService;
    private final RuntimeLifecycleDomainService runtimeLifecycleDomainService;
    private final RuntimeStatusDomainService runtimeStatusDomainService;
    private final ServiceSummaryDomainService serviceSummaryDomainService;

    public ServiceRuntimeDomainService(
            RuntimeDeploymentDomainService runtimeDeploymentDomainService,
            RuntimeLifecycleDomainService runtimeLifecycleDomainService,
            RuntimeStatusDomainService runtimeStatusDomainService,
            ServiceSummaryDomainService serviceSummaryDomainService
    ) {
        this.runtimeDeploymentDomainService = Objects.requireNonNull(
                runtimeDeploymentDomainService,
                "runtimeDeploymentDomainService must not be null"
        );
        this.runtimeLifecycleDomainService = Objects.requireNonNull(
                runtimeLifecycleDomainService,
                "runtimeLifecycleDomainService must not be null"
        );
        this.runtimeStatusDomainService = Objects.requireNonNull(
                runtimeStatusDomainService,
                "runtimeStatusDomainService must not be null"
        );
        this.serviceSummaryDomainService = Objects.requireNonNull(
                serviceSummaryDomainService,
                "serviceSummaryDomainService must not be null"
        );
    }

    ServiceRuntimeDomainService(
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
            UpdateDeploymentStatusPort updateDeploymentStatusPort,
            FindActualDeploymentStatePort findActualDeploymentStatePort,
            FindServiceSummaryCandidatesPort findServiceSummaryCandidatesPort,
            DeploymentMonitor deploymentMonitor
    ) {
        this(servicesFrom(
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
                updateDeploymentStatusPort,
                findActualDeploymentStatePort,
                findServiceSummaryCandidatesPort,
                deploymentMonitor
        ));
    }

    private ServiceRuntimeDomainService(RuntimeServices services) {
        this(
                services.runtimeDeploymentDomainService(),
                services.runtimeLifecycleDomainService(),
                services.runtimeStatusDomainService(),
                services.serviceSummaryDomainService()
        );
    }

    @Override
    public DeployServiceResult deployService(DeployServiceCommand command) {
        return runtimeDeploymentDomainService.deployService(command);
    }

    @Override
    public StartServiceResult startService(StartServiceCommand command) {
        return runtimeLifecycleDomainService.startService(command);
    }

    @Override
    public StopServiceResult stopService(StopServiceCommand command) {
        return runtimeLifecycleDomainService.stopService(command);
    }

    @Override
    public UninstallServiceResult uninstallService(UninstallServiceCommand command) {
        return runtimeLifecycleDomainService.uninstallService(command);
    }

    @Override
    public GetServiceRuntimeStatusResult getServiceRuntimeStatus(GetServiceRuntimeStatusCommand command) {
        return runtimeStatusDomainService.getServiceRuntimeStatus(command);
    }

    @Override
    public GetServiceSummariesResult getServiceSummaries() {
        return serviceSummaryDomainService.getServiceSummaries();
    }

    private static RuntimeServices servicesFrom(
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
            UpdateDeploymentStatusPort updateDeploymentStatusPort,
            FindActualDeploymentStatePort findActualDeploymentStatePort,
            FindServiceSummaryCandidatesPort findServiceSummaryCandidatesPort,
            DeploymentMonitor deploymentMonitor
    ) {
        RuntimeStatusDomainService runtimeStatusDomainService = new RuntimeStatusDomainService(
                findDesiredDeploymentStatePort,
                findActualDeploymentStatePort
        );
        return new RuntimeServices(
                new RuntimeDeploymentDomainService(
                        findServiceDefinitionPort,
                        resolveDeploymentImagePort,
                        recordDeploymentHistoryPort,
                        upsertDesiredDeploymentPort,
                        deployContainerPort,
                        updateDeploymentStatusPort,
                        deploymentMonitor
                ),
                new RuntimeLifecycleDomainService(
                        updateDesiredDeploymentStatePort,
                        findDesiredDeploymentStatePort,
                        startContainerPort,
                        stopContainerPort,
                        removeContainerPort,
                        deleteDesiredDeploymentPort
                ),
                runtimeStatusDomainService,
                new ServiceSummaryDomainService(findServiceSummaryCandidatesPort, runtimeStatusDomainService)
        );
    }

    private record RuntimeServices(
            RuntimeDeploymentDomainService runtimeDeploymentDomainService,
            RuntimeLifecycleDomainService runtimeLifecycleDomainService,
            RuntimeStatusDomainService runtimeStatusDomainService,
            ServiceSummaryDomainService serviceSummaryDomainService
    ) {
    }
}
