package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.ActualDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachment;
import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachments;
import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.PortMappings;
import hr.tvz.popovic.deployko.application.domain.model.RuntimeConfiguration;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.ServiceRuntimeStatus;
import hr.tvz.popovic.deployko.application.domain.model.ServiceSummary;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMounts;
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
import hr.tvz.popovic.deployko.application.port.out.StartContainerPort;
import hr.tvz.popovic.deployko.application.port.out.StopContainerPort;
import hr.tvz.popovic.deployko.application.port.out.UpdateDesiredDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.UpsertDesiredDeploymentPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceRuntimeDomainServiceTest {

    private static final Service SERVICE = service();
    private static final ImageVersion IMAGE_VERSION = new ImageVersion("1.0.0");
    private static final DeploymentId DEPLOYMENT_ID = new DeploymentId(
            UUID.fromString("018f4b5d-9c64-7000-9f2e-4d8fbf9f1b22")
    );

    @Test
    void deploys_service_when_definition_exists_and_ports_succeed() {
        FakeUpsertDesiredDeploymentPort upsertDesiredDeploymentPort = new FakeUpsertDesiredDeploymentPort(
                new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success()
        );
        FakeDeployContainerPort deployContainerPort = new FakeDeployContainerPort(
                new DeployContainerPort.DeployContainerResult.Success()
        );
        FakeRecordDeploymentHistoryPort recordDeploymentHistoryPort = new FakeRecordDeploymentHistoryPort(
                new RecordDeploymentHistoryPort.RecordDeploymentHistoryResult.Recorded(DEPLOYMENT_ID)
        );
        ServiceRuntimeDomainService service = new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.Found(SERVICE),
                upsertDesiredDeploymentPort,
                successfulUpdateStatePort(),
                successfulFindDesiredDeploymentStatePort(),
                deployContainerPort,
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                failingRemoveContainerPort(),
                failingDeleteDesiredDeploymentPort(),
                recordDeploymentHistoryPort,
                successfulFindActualDeploymentStatePort(),
                failingFindServiceSummaryCandidatesPort()
        );

        DeployServiceUseCase.DeployServiceResult result = service.deployService(
                new DeployServiceUseCase.DeployServiceCommand(SERVICE.name(), IMAGE_VERSION)
        );

        assertThat(result).isInstanceOf(DeployServiceUseCase.DeployServiceResult.Success.class);
        assertThat(recordDeploymentHistoryPort.records)
                .containsExactly(new DeploymentHistoryRecord(SERVICE.name(), IMAGE_VERSION));
        assertThat(upsertDesiredDeploymentPort.upsertedDeployments).hasSize(1);
        assertThat(upsertDesiredDeploymentPort.upsertedDeployments.getFirst()).isEqualTo(new DesiredDeployment(
                SERVICE.name(),
                SERVICE.imageRepository(),
                IMAGE_VERSION,
                SERVICE.runtimeConfiguration(),
                DesiredDeploymentState.RUNNING
        ));
        assertThat(deployContainerPort.deployedDeployments)
                .containsExactly(upsertDesiredDeploymentPort.upsertedDeployments.getFirst());
        assertThat(deployContainerPort.deploymentIds).containsExactly(DEPLOYMENT_ID);
    }

    @Test
    void records_deployment_history_before_upserting_desired_state_and_deploying_container() {
        List<String> events = new ArrayList<>();
        ServiceRuntimeDomainService service = serviceWithDeployPorts(
                new FakeRecordDeploymentHistoryPort(
                        new RecordDeploymentHistoryPort.RecordDeploymentHistoryResult.Recorded(DEPLOYMENT_ID),
                        events
                ),
                new FakeUpsertDesiredDeploymentPort(
                        new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success(),
                        events
                ),
                new FakeDeployContainerPort(new DeployContainerPort.DeployContainerResult.Success(), events)
        );

        DeployServiceUseCase.DeployServiceResult result = service.deployService(
                new DeployServiceUseCase.DeployServiceCommand(SERVICE.name(), IMAGE_VERSION)
        );

        assertThat(result).isInstanceOf(DeployServiceUseCase.DeployServiceResult.Success.class);
        assertThat(events).containsExactly("record-history", "upsert-desired-state", "deploy-container");
    }

    @Test
    void returns_desired_state_failure_when_deployment_history_recording_fails() {
        FakeUpsertDesiredDeploymentPort upsertDesiredDeploymentPort = new FakeUpsertDesiredDeploymentPort(
                new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success()
        );
        FakeDeployContainerPort deployContainerPort = new FakeDeployContainerPort(
                new DeployContainerPort.DeployContainerResult.Success()
        );
        ServiceRuntimeDomainService service = serviceWithDeployPorts(
                new FakeRecordDeploymentHistoryPort(
                        new RecordDeploymentHistoryPort.RecordDeploymentHistoryResult.Failure()
                ),
                upsertDesiredDeploymentPort,
                deployContainerPort
        );

        DeployServiceUseCase.DeployServiceResult result = service.deployService(
                new DeployServiceUseCase.DeployServiceCommand(SERVICE.name(), IMAGE_VERSION)
        );

        assertThat(result).isInstanceOf(DeployServiceUseCase.DeployServiceResult.DesiredStateFailure.class);
        assertThat(upsertDesiredDeploymentPort.upsertedDeployments).isEmpty();
        assertThat(deployContainerPort.deployedDeployments).isEmpty();
    }

    @Test
    void returns_service_not_found_when_deployment_history_recording_reports_missing_service() {
        FakeUpsertDesiredDeploymentPort upsertDesiredDeploymentPort = new FakeUpsertDesiredDeploymentPort(
                new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success()
        );
        FakeDeployContainerPort deployContainerPort = new FakeDeployContainerPort(
                new DeployContainerPort.DeployContainerResult.Success()
        );
        ServiceRuntimeDomainService service = serviceWithDeployPorts(
                new FakeRecordDeploymentHistoryPort(
                        new RecordDeploymentHistoryPort.RecordDeploymentHistoryResult.ServiceNotFound()
                ),
                upsertDesiredDeploymentPort,
                deployContainerPort
        );

        DeployServiceUseCase.DeployServiceResult result = service.deployService(
                new DeployServiceUseCase.DeployServiceCommand(SERVICE.name(), IMAGE_VERSION)
        );

        assertThat(result).isInstanceOf(DeployServiceUseCase.DeployServiceResult.ServiceNotFound.class);
        assertThat(upsertDesiredDeploymentPort.upsertedDeployments).isEmpty();
        assertThat(deployContainerPort.deployedDeployments).isEmpty();
    }

    @Test
    void returns_service_not_found_when_service_definition_is_missing() {
        FakeRecordDeploymentHistoryPort recordDeploymentHistoryPort = new FakeRecordDeploymentHistoryPort(
                new RecordDeploymentHistoryPort.RecordDeploymentHistoryResult.Recorded(DEPLOYMENT_ID)
        );
        ServiceRuntimeDomainService service = new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.NotFound(),
                _ -> new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success(),
                successfulUpdateStatePort(),
                successfulFindDesiredDeploymentStatePort(),
                (_, _) -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                failingRemoveContainerPort(),
                failingDeleteDesiredDeploymentPort(),
                recordDeploymentHistoryPort,
                successfulFindActualDeploymentStatePort(),
                failingFindServiceSummaryCandidatesPort()
        );

        DeployServiceUseCase.DeployServiceResult result = service.deployService(
                new DeployServiceUseCase.DeployServiceCommand(SERVICE.name(), IMAGE_VERSION)
        );

        assertThat(result).isInstanceOf(DeployServiceUseCase.DeployServiceResult.ServiceNotFound.class);
        assertThat(recordDeploymentHistoryPort.records).isEmpty();
    }

    @Test
    void returns_desired_state_failure_when_service_definition_lookup_fails() {
        ServiceRuntimeDomainService service = new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.Failure(),
                _ -> new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success(),
                successfulUpdateStatePort(),
                successfulFindDesiredDeploymentStatePort(),
                (_, _) -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                successfulFindActualDeploymentStatePort()
        );

        DeployServiceUseCase.DeployServiceResult result = service.deployService(
                new DeployServiceUseCase.DeployServiceCommand(SERVICE.name(), IMAGE_VERSION)
        );

        assertThat(result).isInstanceOf(DeployServiceUseCase.DeployServiceResult.DesiredStateFailure.class);
    }

    @Test
    void returns_service_not_found_when_upsert_reports_missing_service() {
        ServiceRuntimeDomainService service = new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.Found(SERVICE),
                _ -> new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.ServiceNotFound(),
                successfulUpdateStatePort(),
                successfulFindDesiredDeploymentStatePort(),
                (_, _) -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                successfulFindActualDeploymentStatePort()
        );

        DeployServiceUseCase.DeployServiceResult result = service.deployService(
                new DeployServiceUseCase.DeployServiceCommand(SERVICE.name(), IMAGE_VERSION)
        );

        assertThat(result).isInstanceOf(DeployServiceUseCase.DeployServiceResult.ServiceNotFound.class);
    }

    @Test
    void returns_desired_state_failure_when_upsert_fails() {
        ServiceRuntimeDomainService service = new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.Found(SERVICE),
                _ -> new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Failure(),
                successfulUpdateStatePort(),
                successfulFindDesiredDeploymentStatePort(),
                (_, _) -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                successfulFindActualDeploymentStatePort()
        );

        DeployServiceUseCase.DeployServiceResult result = service.deployService(
                new DeployServiceUseCase.DeployServiceCommand(SERVICE.name(), IMAGE_VERSION)
        );

        assertThat(result).isInstanceOf(DeployServiceUseCase.DeployServiceResult.DesiredStateFailure.class);
    }

    @Test
    void returns_docker_failure_when_deploy_port_fails() {
        ServiceRuntimeDomainService service = new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.Found(SERVICE),
                _ -> new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success(),
                successfulUpdateStatePort(),
                successfulFindDesiredDeploymentStatePort(),
                (_, _) -> new DeployContainerPort.DeployContainerResult.Failure(),
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                successfulFindActualDeploymentStatePort()
        );

        DeployServiceUseCase.DeployServiceResult result = service.deployService(
                new DeployServiceUseCase.DeployServiceCommand(SERVICE.name(), IMAGE_VERSION)
        );

        assertThat(result).isInstanceOf(DeployServiceUseCase.DeployServiceResult.DockerFailure.class);
    }

    @Test
    void starts_service_when_state_update_and_start_port_succeed() {
        ServiceRuntimeDomainService service = new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.NotFound(),
                _ -> new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success(),
                successfulUpdateStatePort(),
                successfulFindDesiredDeploymentStatePort(),
                (_, _) -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                successfulFindActualDeploymentStatePort()
        );

        StartServiceUseCase.StartServiceResult result = service.startService(
                new StartServiceUseCase.StartServiceCommand(SERVICE.name())
        );

        assertThat(result).isInstanceOf(StartServiceUseCase.StartServiceResult.Success.class);
    }

    @Test
    void stops_service_when_state_update_and_stop_port_succeed() {
        ServiceRuntimeDomainService service = new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.NotFound(),
                _ -> new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success(),
                successfulUpdateStatePort(),
                successfulFindDesiredDeploymentStatePort(),
                (_, _) -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                successfulFindActualDeploymentStatePort()
        );

        StopServiceUseCase.StopServiceResult result = service.stopService(
                new StopServiceUseCase.StopServiceCommand(SERVICE.name())
        );

        assertThat(result).isInstanceOf(StopServiceUseCase.StopServiceResult.Success.class);
    }

    @Test
    void uninstalls_service_when_desired_deployment_exists_and_container_is_removed() {
        FakeRemoveContainerPort removeContainerPort = new FakeRemoveContainerPort(
                new RemoveContainerPort.RemoveContainerResult.Success()
        );
        FakeDeleteDesiredDeploymentPort deleteDesiredDeploymentPort = new FakeDeleteDesiredDeploymentPort(
                new DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult.Deleted()
        );
        ServiceRuntimeDomainService service = serviceWithUninstallPorts(
                successfulFindDesiredDeploymentStatePort(),
                removeContainerPort,
                deleteDesiredDeploymentPort
        );

        UninstallServiceUseCase.UninstallServiceResult result = service.uninstallService(
                new UninstallServiceUseCase.UninstallServiceCommand(SERVICE.name())
        );

        assertThat(result).isInstanceOf(UninstallServiceUseCase.UninstallServiceResult.Success.class);
        assertThat(removeContainerPort.removedServiceNames).containsExactly(SERVICE.name());
        assertThat(deleteDesiredDeploymentPort.deletedServiceNames).containsExactly(SERVICE.name());
    }

    @Test
    void uninstalls_service_when_desired_deployment_exists_and_container_is_missing() {
        FakeDeleteDesiredDeploymentPort deleteDesiredDeploymentPort = new FakeDeleteDesiredDeploymentPort(
                new DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult.Deleted()
        );
        ServiceRuntimeDomainService service = serviceWithUninstallPorts(
                successfulFindDesiredDeploymentStatePort(),
                new FakeRemoveContainerPort(new RemoveContainerPort.RemoveContainerResult.MissingContainer()),
                deleteDesiredDeploymentPort
        );

        UninstallServiceUseCase.UninstallServiceResult result = service.uninstallService(
                new UninstallServiceUseCase.UninstallServiceCommand(SERVICE.name())
        );

        assertThat(result).isInstanceOf(UninstallServiceUseCase.UninstallServiceResult.Success.class);
        assertThat(deleteDesiredDeploymentPort.deletedServiceNames).containsExactly(SERVICE.name());
    }

    @Test
    void removes_orphaned_container_when_no_desired_deployment_exists() {
        FakeDeleteDesiredDeploymentPort deleteDesiredDeploymentPort = new FakeDeleteDesiredDeploymentPort(
                new DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult.Deleted()
        );
        ServiceRuntimeDomainService service = serviceWithUninstallPorts(
                _ -> new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.NotDeployed(),
                new FakeRemoveContainerPort(new RemoveContainerPort.RemoveContainerResult.Success()),
                deleteDesiredDeploymentPort
        );

        UninstallServiceUseCase.UninstallServiceResult result = service.uninstallService(
                new UninstallServiceUseCase.UninstallServiceCommand(SERVICE.name())
        );

        assertThat(result).isInstanceOf(UninstallServiceUseCase.UninstallServiceResult.Success.class);
        assertThat(deleteDesiredDeploymentPort.deletedServiceNames).isEmpty();
    }

    @Test
    void returns_not_deployed_when_no_desired_deployment_or_container_exists() {
        ServiceRuntimeDomainService service = serviceWithUninstallPorts(
                _ -> new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.NotDeployed(),
                new FakeRemoveContainerPort(new RemoveContainerPort.RemoveContainerResult.MissingContainer()),
                new FakeDeleteDesiredDeploymentPort(new DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult.Deleted())
        );

        UninstallServiceUseCase.UninstallServiceResult result = service.uninstallService(
                new UninstallServiceUseCase.UninstallServiceCommand(SERVICE.name())
        );

        assertThat(result).isInstanceOf(UninstallServiceUseCase.UninstallServiceResult.NotDeployed.class);
    }

    @Test
    void returns_drift_when_uninstall_finds_duplicate_managed_containers() {
        FakeDeleteDesiredDeploymentPort deleteDesiredDeploymentPort = new FakeDeleteDesiredDeploymentPort(
                new DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult.Deleted()
        );
        ServiceRuntimeDomainService service = serviceWithUninstallPorts(
                successfulFindDesiredDeploymentStatePort(),
                new FakeRemoveContainerPort(new RemoveContainerPort.RemoveContainerResult.DuplicateManagedContainers()),
                deleteDesiredDeploymentPort
        );

        UninstallServiceUseCase.UninstallServiceResult result = service.uninstallService(
                new UninstallServiceUseCase.UninstallServiceCommand(SERVICE.name())
        );

        assertThat(result).isInstanceOf(UninstallServiceUseCase.UninstallServiceResult.Drift.class);
        assertThat(deleteDesiredDeploymentPort.deletedServiceNames).isEmpty();
    }

    @Test
    void returns_docker_failure_when_uninstall_container_remove_fails() {
        ServiceRuntimeDomainService service = serviceWithUninstallPorts(
                successfulFindDesiredDeploymentStatePort(),
                new FakeRemoveContainerPort(new RemoveContainerPort.RemoveContainerResult.Failure()),
                new FakeDeleteDesiredDeploymentPort(new DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult.Deleted())
        );

        UninstallServiceUseCase.UninstallServiceResult result = service.uninstallService(
                new UninstallServiceUseCase.UninstallServiceCommand(SERVICE.name())
        );

        assertThat(result).isInstanceOf(UninstallServiceUseCase.UninstallServiceResult.DockerFailure.class);
    }

    @Test
    void returns_desired_state_failure_when_uninstall_desired_deployment_delete_fails() {
        ServiceRuntimeDomainService service = serviceWithUninstallPorts(
                successfulFindDesiredDeploymentStatePort(),
                new FakeRemoveContainerPort(new RemoveContainerPort.RemoveContainerResult.Success()),
                new FakeDeleteDesiredDeploymentPort(new DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult.Failure())
        );

        UninstallServiceUseCase.UninstallServiceResult result = service.uninstallService(
                new UninstallServiceUseCase.UninstallServiceCommand(SERVICE.name())
        );

        assertThat(result).isInstanceOf(UninstallServiceUseCase.UninstallServiceResult.DesiredStateFailure.class);
    }

    @Test
    void returns_service_not_found_when_uninstall_desired_state_lookup_reports_missing_service() {
        ServiceRuntimeDomainService service = serviceWithUninstallPorts(
                _ -> new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.ServiceNotFound(),
                new FakeRemoveContainerPort(new RemoveContainerPort.RemoveContainerResult.Success()),
                new FakeDeleteDesiredDeploymentPort(new DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult.Deleted())
        );

        UninstallServiceUseCase.UninstallServiceResult result = service.uninstallService(
                new UninstallServiceUseCase.UninstallServiceCommand(SERVICE.name())
        );

        assertThat(result).isInstanceOf(UninstallServiceUseCase.UninstallServiceResult.ServiceNotFound.class);
    }

    @Test
    void returns_status_for_each_desired_and_actual_state_combination() {
        List<StatusCase> statusCases = List.of(
                new StatusCase(DesiredDeploymentState.RUNNING, ActualDeploymentState.RUNNING, ServiceRuntimeStatus.RUNNING),
                new StatusCase(
                        DesiredDeploymentState.RUNNING,
                        ActualDeploymentState.STOPPED,
                        ServiceRuntimeStatus.EXPECTED_RUNNING_BUT_STOPPED
                ),
                new StatusCase(
                        DesiredDeploymentState.RUNNING,
                        ActualDeploymentState.MISSING,
                        ServiceRuntimeStatus.EXPECTED_RUNNING_BUT_MISSING
                ),
                new StatusCase(
                        DesiredDeploymentState.STOPPED,
                        ActualDeploymentState.RUNNING,
                        ServiceRuntimeStatus.EXPECTED_STOPPED_BUT_RUNNING
                ),
                new StatusCase(DesiredDeploymentState.STOPPED, ActualDeploymentState.STOPPED, ServiceRuntimeStatus.STOPPED),
                new StatusCase(
                        DesiredDeploymentState.STOPPED,
                        ActualDeploymentState.MISSING,
                        ServiceRuntimeStatus.EXPECTED_STOPPED_BUT_MISSING
                )
        );

        for (StatusCase statusCase : statusCases) {
            ServiceRuntimeDomainService service = serviceWithStatusPorts(
                    _ -> new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Found(
                            statusCase.desiredState()
                    ),
                    _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found(
                            statusCase.actualState()
                    )
            );

            GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult result = service.getServiceRuntimeStatus(
                    new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusCommand(SERVICE.name())
            );

            assertThat(result)
                    .as("desired %s actual %s", statusCase.desiredState(), statusCase.actualState())
                    .isEqualTo(new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult.Success(
                            statusCase.expectedStatus()
                    ));
        }
    }

    @Test
    void returns_status_for_services_without_desired_deployment() {
        List<UndeployedStatusCase> statusCases = List.of(
                new UndeployedStatusCase(ActualDeploymentState.RUNNING, ServiceRuntimeStatus.ORPHANED_RUNNING),
                new UndeployedStatusCase(ActualDeploymentState.STOPPED, ServiceRuntimeStatus.ORPHANED_STOPPED),
                new UndeployedStatusCase(ActualDeploymentState.MISSING, ServiceRuntimeStatus.NOT_DEPLOYED)
        );

        for (UndeployedStatusCase statusCase : statusCases) {
            ServiceRuntimeDomainService service = serviceWithStatusPorts(
                    _ -> new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.NotDeployed(),
                    _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found(
                            statusCase.actualState()
                    )
            );

            GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult result = service.getServiceRuntimeStatus(
                    new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusCommand(SERVICE.name())
            );

            assertThat(result)
                    .as("actual %s", statusCase.actualState())
                    .isEqualTo(new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult.Success(
                            statusCase.expectedStatus()
                    ));
        }
    }

    @Test
    void returns_duplicate_managed_containers_status_when_docker_reports_duplicates() {
        ServiceRuntimeDomainService service = serviceWithStatusPorts(
                _ -> new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Found(
                        DesiredDeploymentState.RUNNING
                ),
                _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.DuplicateManagedContainers()
        );

        GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult result = service.getServiceRuntimeStatus(
                new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusCommand(SERVICE.name())
        );

        assertThat(result).isEqualTo(new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult.Success(
                ServiceRuntimeStatus.DUPLICATE_MANAGED_CONTAINERS
        ));
    }

    @Test
    void returns_service_not_found_when_status_desired_state_lookup_reports_missing_service() {
        ServiceRuntimeDomainService service = serviceWithStatusPorts(
                _ -> new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.ServiceNotFound(),
                successfulFindActualDeploymentStatePort()
        );

        GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult result = service.getServiceRuntimeStatus(
                new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusCommand(SERVICE.name())
        );

        assertThat(result)
                .isInstanceOf(GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult.ServiceNotFound.class);
    }

    @Test
    void returns_desired_state_failure_when_status_desired_state_lookup_fails() {
        ServiceRuntimeDomainService service = serviceWithStatusPorts(
                _ -> new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Failure(),
                successfulFindActualDeploymentStatePort()
        );

        GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult result = service.getServiceRuntimeStatus(
                new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusCommand(SERVICE.name())
        );

        assertThat(result)
                .isInstanceOf(GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult.DesiredStateFailure.class);
    }

    @Test
    void returns_docker_failure_when_status_actual_state_lookup_fails() {
        ServiceRuntimeDomainService service = serviceWithStatusPorts(
                successfulFindDesiredDeploymentStatePort(),
                _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Failure()
        );

        GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult result = service.getServiceRuntimeStatus(
                new GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusCommand(SERVICE.name())
        );

        assertThat(result)
                .isInstanceOf(GetServiceRuntimeStatusUseCase.GetServiceRuntimeStatusResult.DockerFailure.class);
    }

    @Test
    void returns_service_summaries_with_deployed_version_and_status() {
        ServiceName undeployedServiceName = new ServiceName("billing-api");
        ServiceRuntimeDomainService service = serviceWithSummaryPorts(
                () -> new FindServiceSummaryCandidatesPort.FindServiceSummaryCandidatesResult.Found(List.of(
                        new FindServiceSummaryCandidatesPort.ServiceSummaryCandidate(
                                SERVICE.name(),
                                SERVICE.imageRepository(),
                                Optional.of(IMAGE_VERSION),
                                Optional.of(DesiredDeploymentState.RUNNING)
                        ),
                        new FindServiceSummaryCandidatesPort.ServiceSummaryCandidate(
                                undeployedServiceName,
                                new ImageRepository("ghcr.io/deployko/billing-api"),
                                Optional.empty(),
                                Optional.empty()
                        )
                )),
                serviceName -> {
                    if (serviceName.equals(undeployedServiceName)) {
                        return new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found(
                                ActualDeploymentState.MISSING
                        );
                    }
                    return new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found(
                            ActualDeploymentState.RUNNING
                    );
                }
        );

        GetServiceSummariesUseCase.GetServiceSummariesResult result = service.getServiceSummaries();

        assertThat(result).isInstanceOf(GetServiceSummariesUseCase.GetServiceSummariesResult.Success.class);
        GetServiceSummariesUseCase.GetServiceSummariesResult.Success success =
                (GetServiceSummariesUseCase.GetServiceSummariesResult.Success) result;
        assertThat(success.services()).containsExactly(
                new ServiceSummary(
                        SERVICE.name(),
                        SERVICE.imageRepository(),
                        Optional.of(IMAGE_VERSION),
                        ServiceRuntimeStatus.RUNNING
                ),
                new ServiceSummary(
                        undeployedServiceName,
                        new ImageRepository("ghcr.io/deployko/billing-api"),
                        Optional.empty(),
                        ServiceRuntimeStatus.NOT_DEPLOYED
                )
        );
    }

    @Test
    void returns_service_summaries_with_drift_status_when_docker_reports_duplicates() {
        ServiceRuntimeDomainService service = serviceWithSummaryPorts(
                () -> new FindServiceSummaryCandidatesPort.FindServiceSummaryCandidatesResult.Found(List.of(
                        new FindServiceSummaryCandidatesPort.ServiceSummaryCandidate(
                                SERVICE.name(),
                                SERVICE.imageRepository(),
                                Optional.of(IMAGE_VERSION),
                                Optional.of(DesiredDeploymentState.RUNNING)
                        )
                )),
                _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.DuplicateManagedContainers()
        );

        GetServiceSummariesUseCase.GetServiceSummariesResult result = service.getServiceSummaries();

        assertThat(result).isInstanceOf(GetServiceSummariesUseCase.GetServiceSummariesResult.Success.class);
        GetServiceSummariesUseCase.GetServiceSummariesResult.Success success =
                (GetServiceSummariesUseCase.GetServiceSummariesResult.Success) result;
        assertThat(success.services().getFirst().status()).isEqualTo(ServiceRuntimeStatus.DUPLICATE_MANAGED_CONTAINERS);
    }

    @Test
    void returns_summary_failure_when_candidate_lookup_fails() {
        ServiceRuntimeDomainService service = serviceWithSummaryPorts(
                () -> new FindServiceSummaryCandidatesPort.FindServiceSummaryCandidatesResult.Failure(),
                successfulFindActualDeploymentStatePort()
        );

        GetServiceSummariesUseCase.GetServiceSummariesResult result = service.getServiceSummaries();

        assertThat(result).isInstanceOf(GetServiceSummariesUseCase.GetServiceSummariesResult.Failure.class);
    }

    @Test
    void returns_summary_failure_when_actual_state_lookup_fails() {
        ServiceRuntimeDomainService service = serviceWithSummaryPorts(
                () -> new FindServiceSummaryCandidatesPort.FindServiceSummaryCandidatesResult.Found(List.of(
                        new FindServiceSummaryCandidatesPort.ServiceSummaryCandidate(
                                SERVICE.name(),
                                SERVICE.imageRepository(),
                                Optional.of(IMAGE_VERSION),
                                Optional.of(DesiredDeploymentState.RUNNING)
                        )
                )),
                _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Failure()
        );

        GetServiceSummariesUseCase.GetServiceSummariesResult result = service.getServiceSummaries();

        assertThat(result).isInstanceOf(GetServiceSummariesUseCase.GetServiceSummariesResult.Failure.class);
    }

    private static Service service() {
        EnvironmentVariables environmentVariables = EnvironmentVariables.empty()
                .add(new EnvironmentVariables.Key("APP_ENV"), new EnvironmentVariables.Value("prod"));

        PortMappings portMappings = PortMappings.empty()
                .add(new Port(8080), new Port(80));

        VolumeMounts volumeMounts = VolumeMounts.empty()
                .add(new VolumeMount.NamedVolumeMount(
                        new VolumeMount.VolumeName("deployko_data"),
                        new VolumeMount.Target("/var/lib/deployko"),
                        false
                ));

        NetworkAttachments networkAttachments = NetworkAttachments.empty()
                .add(new NetworkAttachment(new NetworkAttachment.NetworkName("backend")));

        return new Service(
                new ServiceName("deployko-api"),
                new ImageRepository("ghcr.io/deployko/api"),
                new RuntimeConfiguration(environmentVariables, portMappings, volumeMounts, networkAttachments)
        );
    }

    private static UpdateDesiredDeploymentStatePort successfulUpdateStatePort() {
        return (_, _) -> new UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.Success();
    }

    private static FindDesiredDeploymentStatePort successfulFindDesiredDeploymentStatePort() {
        return _ -> new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Found(
                DesiredDeploymentState.RUNNING
        );
    }

    private static StartContainerPort successfulStartContainerPort() {
        return _ -> new StartContainerPort.StartContainerResult.Success();
    }

    private static StopContainerPort successfulStopContainerPort() {
        return _ -> new StopContainerPort.StopContainerResult.Success();
    }

    private static FindActualDeploymentStatePort successfulFindActualDeploymentStatePort() {
        return _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found(
                ActualDeploymentState.RUNNING
        );
    }

    private static RemoveContainerPort failingRemoveContainerPort() {
        return _ -> new RemoveContainerPort.RemoveContainerResult.Failure();
    }

    private static DeleteDesiredDeploymentPort failingDeleteDesiredDeploymentPort() {
        return _ -> new DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult.Failure();
    }

    private static FindServiceSummaryCandidatesPort failingFindServiceSummaryCandidatesPort() {
        return () -> new FindServiceSummaryCandidatesPort.FindServiceSummaryCandidatesResult.Failure();
    }

    private static ServiceRuntimeDomainService serviceWithDeployPorts(
            RecordDeploymentHistoryPort recordDeploymentHistoryPort,
            UpsertDesiredDeploymentPort upsertDesiredDeploymentPort,
            DeployContainerPort deployContainerPort
    ) {
        return new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.Found(SERVICE),
                upsertDesiredDeploymentPort,
                successfulUpdateStatePort(),
                successfulFindDesiredDeploymentStatePort(),
                deployContainerPort,
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                failingRemoveContainerPort(),
                failingDeleteDesiredDeploymentPort(),
                recordDeploymentHistoryPort,
                successfulFindActualDeploymentStatePort(),
                failingFindServiceSummaryCandidatesPort()
        );
    }

    private static ServiceRuntimeDomainService serviceWithStatusPorts(
            FindDesiredDeploymentStatePort findDesiredDeploymentStatePort,
            FindActualDeploymentStatePort findActualDeploymentStatePort
    ) {
        return new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.NotFound(),
                _ -> new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success(),
                successfulUpdateStatePort(),
                findDesiredDeploymentStatePort,
                (_, _) -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                findActualDeploymentStatePort
        );
    }

    private static ServiceRuntimeDomainService serviceWithSummaryPorts(
            FindServiceSummaryCandidatesPort findServiceSummaryCandidatesPort,
            FindActualDeploymentStatePort findActualDeploymentStatePort
    ) {
        return new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.NotFound(),
                _ -> new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success(),
                successfulUpdateStatePort(),
                successfulFindDesiredDeploymentStatePort(),
                (_, _) -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                findActualDeploymentStatePort,
                findServiceSummaryCandidatesPort
        );
    }

    private static ServiceRuntimeDomainService serviceWithUninstallPorts(
            FindDesiredDeploymentStatePort findDesiredDeploymentStatePort,
            RemoveContainerPort removeContainerPort,
            DeleteDesiredDeploymentPort deleteDesiredDeploymentPort
    ) {
        return new ServiceRuntimeDomainService(
                _ -> new FindServiceDefinitionPort.FindServiceDefinitionResult.NotFound(),
                _ -> new UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success(),
                successfulUpdateStatePort(),
                findDesiredDeploymentStatePort,
                (_, _) -> new DeployContainerPort.DeployContainerResult.Success(),
                successfulStartContainerPort(),
                successfulStopContainerPort(),
                removeContainerPort,
                deleteDesiredDeploymentPort,
                successfulFindActualDeploymentStatePort()
        );
    }

    private record StatusCase(
            DesiredDeploymentState desiredState,
            ActualDeploymentState actualState,
            ServiceRuntimeStatus expectedStatus
    ) {
    }

    private record UndeployedStatusCase(
            ActualDeploymentState actualState,
            ServiceRuntimeStatus expectedStatus
    ) {
    }

    private record DeploymentHistoryRecord(ServiceName serviceName, ImageVersion imageVersion) {
    }

    private static final class FakeUpsertDesiredDeploymentPort implements UpsertDesiredDeploymentPort {

        private final UpsertDesiredDeploymentResult result;
        private final List<String> events;
        private final List<DesiredDeployment> upsertedDeployments = new ArrayList<>();

        private FakeUpsertDesiredDeploymentPort(UpsertDesiredDeploymentResult result) {
            this(result, new ArrayList<>());
        }

        private FakeUpsertDesiredDeploymentPort(UpsertDesiredDeploymentResult result, List<String> events) {
            this.result = result;
            this.events = events;
        }

        @Override
        public UpsertDesiredDeploymentResult upsert(DesiredDeployment desiredDeployment) {
            events.add("upsert-desired-state");
            upsertedDeployments.add(desiredDeployment);
            return result;
        }
    }

    private static final class FakeDeployContainerPort implements DeployContainerPort {

        private final DeployContainerResult result;
        private final List<String> events;
        private final List<DesiredDeployment> deployedDeployments = new ArrayList<>();
        private final List<DeploymentId> deploymentIds = new ArrayList<>();

        private FakeDeployContainerPort(DeployContainerResult result) {
            this(result, new ArrayList<>());
        }

        private FakeDeployContainerPort(DeployContainerResult result, List<String> events) {
            this.result = result;
            this.events = events;
        }

        @Override
        public DeployContainerResult deploy(DesiredDeployment desiredDeployment, DeploymentId deploymentId) {
            events.add("deploy-container");
            deployedDeployments.add(desiredDeployment);
            deploymentIds.add(deploymentId);
            return result;
        }
    }

    private static final class FakeRecordDeploymentHistoryPort implements RecordDeploymentHistoryPort {

        private final RecordDeploymentHistoryResult result;
        private final List<String> events;
        private final List<DeploymentHistoryRecord> records = new ArrayList<>();

        private FakeRecordDeploymentHistoryPort(RecordDeploymentHistoryResult result) {
            this(result, new ArrayList<>());
        }

        private FakeRecordDeploymentHistoryPort(RecordDeploymentHistoryResult result, List<String> events) {
            this.result = result;
            this.events = events;
        }

        @Override
        public RecordDeploymentHistoryResult recordDeployment(ServiceName serviceName, ImageVersion imageVersion) {
            events.add("record-history");
            records.add(new DeploymentHistoryRecord(serviceName, imageVersion));
            return result;
        }
    }

    private static final class FakeRemoveContainerPort implements RemoveContainerPort {

        private final RemoveContainerResult result;
        private final List<ServiceName> removedServiceNames = new ArrayList<>();

        private FakeRemoveContainerPort(RemoveContainerResult result) {
            this.result = result;
        }

        @Override
        public RemoveContainerResult remove(ServiceName serviceName) {
            removedServiceNames.add(serviceName);
            return result;
        }
    }

    private static final class FakeDeleteDesiredDeploymentPort implements DeleteDesiredDeploymentPort {

        private final DeleteDesiredDeploymentResult result;
        private final List<ServiceName> deletedServiceNames = new ArrayList<>();

        private FakeDeleteDesiredDeploymentPort(DeleteDesiredDeploymentResult result) {
            this.result = result;
        }

        @Override
        public DeleteDesiredDeploymentResult delete(ServiceName serviceName) {
            deletedServiceNames.add(serviceName);
            return result;
        }
    }
}
