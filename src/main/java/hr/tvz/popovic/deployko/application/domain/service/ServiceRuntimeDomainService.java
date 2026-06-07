package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.ActualDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.ServiceRuntimeStatus;
import hr.tvz.popovic.deployko.application.domain.model.ServiceSummary;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Optional;

public final class ServiceRuntimeDomainService
        implements DeployServiceUseCase, StartServiceUseCase, StopServiceUseCase, GetServiceRuntimeStatusUseCase,
        GetServiceSummariesUseCase, UninstallServiceUseCase {

    private final FindServiceDefinitionPort findServiceDefinitionPort;
    private final UpsertDesiredDeploymentPort upsertDesiredDeploymentPort;
    private final UpdateDesiredDeploymentStatePort updateDesiredDeploymentStatePort;
    private final FindDesiredDeploymentStatePort findDesiredDeploymentStatePort;
    private final DeployContainerPort deployContainerPort;
    private final StartContainerPort startContainerPort;
    private final StopContainerPort stopContainerPort;
    private final RemoveContainerPort removeContainerPort;
    private final DeleteDesiredDeploymentPort deleteDesiredDeploymentPort;
    private final RecordDeploymentHistoryPort recordDeploymentHistoryPort;
    private final FindActualDeploymentStatePort findActualDeploymentStatePort;
    private final FindServiceSummaryCandidatesPort findServiceSummaryCandidatesPort;

    public ServiceRuntimeDomainService(
            FindServiceDefinitionPort findServiceDefinitionPort,
            UpsertDesiredDeploymentPort upsertDesiredDeploymentPort,
            UpdateDesiredDeploymentStatePort updateDesiredDeploymentStatePort,
            FindDesiredDeploymentStatePort findDesiredDeploymentStatePort,
            DeployContainerPort deployContainerPort,
            StartContainerPort startContainerPort,
            StopContainerPort stopContainerPort,
            FindActualDeploymentStatePort findActualDeploymentStatePort
    ) {
        this(
                findServiceDefinitionPort,
                upsertDesiredDeploymentPort,
                updateDesiredDeploymentStatePort,
                findDesiredDeploymentStatePort,
                deployContainerPort,
                startContainerPort,
                stopContainerPort,
                _ -> new RemoveContainerPort.RemoveContainerResult.Failure(),
                _ -> new DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult.Failure(),
                (_, _) -> new RecordDeploymentHistoryPort.RecordDeploymentHistoryResult.Recorded(
                        new DeploymentId(UUID.randomUUID())
                ),
                findActualDeploymentStatePort,
                () -> new FindServiceSummaryCandidatesPort.FindServiceSummaryCandidatesResult.Failure()
        );
    }

    public ServiceRuntimeDomainService(
            FindServiceDefinitionPort findServiceDefinitionPort,
            UpsertDesiredDeploymentPort upsertDesiredDeploymentPort,
            UpdateDesiredDeploymentStatePort updateDesiredDeploymentStatePort,
            FindDesiredDeploymentStatePort findDesiredDeploymentStatePort,
            DeployContainerPort deployContainerPort,
            StartContainerPort startContainerPort,
            StopContainerPort stopContainerPort,
            RemoveContainerPort removeContainerPort,
            DeleteDesiredDeploymentPort deleteDesiredDeploymentPort,
            FindActualDeploymentStatePort findActualDeploymentStatePort
    ) {
        this(
                findServiceDefinitionPort,
                upsertDesiredDeploymentPort,
                updateDesiredDeploymentStatePort,
                findDesiredDeploymentStatePort,
                deployContainerPort,
                startContainerPort,
                stopContainerPort,
                removeContainerPort,
                deleteDesiredDeploymentPort,
                (_, _) -> new RecordDeploymentHistoryPort.RecordDeploymentHistoryResult.Recorded(
                        new DeploymentId(UUID.randomUUID())
                ),
                findActualDeploymentStatePort,
                () -> new FindServiceSummaryCandidatesPort.FindServiceSummaryCandidatesResult.Failure()
        );
    }

    public ServiceRuntimeDomainService(
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
        this(
                findServiceDefinitionPort,
                upsertDesiredDeploymentPort,
                updateDesiredDeploymentStatePort,
                findDesiredDeploymentStatePort,
                deployContainerPort,
                startContainerPort,
                stopContainerPort,
                _ -> new RemoveContainerPort.RemoveContainerResult.Failure(),
                _ -> new DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult.Failure(),
                (_, _) -> new RecordDeploymentHistoryPort.RecordDeploymentHistoryResult.Recorded(
                        new DeploymentId(UUID.randomUUID())
                ),
                findActualDeploymentStatePort,
                findServiceSummaryCandidatesPort
        );
    }

    public ServiceRuntimeDomainService(
            FindServiceDefinitionPort findServiceDefinitionPort,
            UpsertDesiredDeploymentPort upsertDesiredDeploymentPort,
            UpdateDesiredDeploymentStatePort updateDesiredDeploymentStatePort,
            FindDesiredDeploymentStatePort findDesiredDeploymentStatePort,
            DeployContainerPort deployContainerPort,
            StartContainerPort startContainerPort,
            StopContainerPort stopContainerPort,
            RemoveContainerPort removeContainerPort,
            DeleteDesiredDeploymentPort deleteDesiredDeploymentPort,
            RecordDeploymentHistoryPort recordDeploymentHistoryPort,
            FindActualDeploymentStatePort findActualDeploymentStatePort,
            FindServiceSummaryCandidatesPort findServiceSummaryCandidatesPort
    ) {
        this.findServiceDefinitionPort = Objects.requireNonNull(
                findServiceDefinitionPort,
                "findServiceDefinitionPort must not be null"
        );
        this.upsertDesiredDeploymentPort = Objects.requireNonNull(
                upsertDesiredDeploymentPort,
                "upsertDesiredDeploymentPort must not be null"
        );
        this.updateDesiredDeploymentStatePort = Objects.requireNonNull(
                updateDesiredDeploymentStatePort,
                "updateDesiredDeploymentStatePort must not be null"
        );
        this.findDesiredDeploymentStatePort = Objects.requireNonNull(
                findDesiredDeploymentStatePort,
                "findDesiredDeploymentStatePort must not be null"
        );
        this.deployContainerPort = Objects.requireNonNull(
                deployContainerPort,
                "deployContainerPort must not be null"
        );
        this.startContainerPort = Objects.requireNonNull(
                startContainerPort,
                "startContainerPort must not be null"
        );
        this.stopContainerPort = Objects.requireNonNull(
                stopContainerPort,
                "stopContainerPort must not be null"
        );
        this.removeContainerPort = Objects.requireNonNull(
                removeContainerPort,
                "removeContainerPort must not be null"
        );
        this.deleteDesiredDeploymentPort = Objects.requireNonNull(
                deleteDesiredDeploymentPort,
                "deleteDesiredDeploymentPort must not be null"
        );
        this.recordDeploymentHistoryPort = Objects.requireNonNull(
                recordDeploymentHistoryPort,
                "recordDeploymentHistoryPort must not be null"
        );
        this.findActualDeploymentStatePort = Objects.requireNonNull(
                findActualDeploymentStatePort,
                "findActualDeploymentStatePort must not be null"
        );
        this.findServiceSummaryCandidatesPort = Objects.requireNonNull(
                findServiceSummaryCandidatesPort,
                "findServiceSummaryCandidatesPort must not be null"
        );
    }

    @Override
    public DeployServiceResult deployService(DeployServiceCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (findServiceDefinitionPort.findByName(command.serviceName())) {
            case FindServiceDefinitionPort.FindServiceDefinitionResult.Found found ->
                    deployFoundService(found.service(), command.imageVersion());
            case FindServiceDefinitionPort.FindServiceDefinitionResult.NotFound _ ->
                    new DeployServiceResult.ServiceNotFound();
            case FindServiceDefinitionPort.FindServiceDefinitionResult.Failure _ ->
                    new DeployServiceResult.DesiredStateFailure();
        };
    }

    @Override
    public StartServiceResult startService(StartServiceCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (updateDesiredDeploymentStatePort.updateState(command.serviceName(), DesiredDeploymentState.RUNNING)) {
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.Success _ ->
                    switch (startContainerPort.start(command.serviceName())) {
                        case StartContainerPort.StartContainerResult.Success _ -> new StartServiceResult.Success();
                        case StartContainerPort.StartContainerResult.MissingContainer _ -> new StartServiceResult.NotDeployed();
                        case StartContainerPort.StartContainerResult.DuplicateManagedContainers _ -> new StartServiceResult.Drift();
                        case StartContainerPort.StartContainerResult.Failure _ -> new StartServiceResult.DockerFailure();
                    };
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.ServiceNotFound _ ->
                    new StartServiceResult.ServiceNotFound();
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.NotDeployed _ ->
                    new StartServiceResult.NotDeployed();
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.Failure _ ->
                    new StartServiceResult.DesiredStateFailure();
        };
    }

    @Override
    public StopServiceResult stopService(StopServiceCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (updateDesiredDeploymentStatePort.updateState(command.serviceName(), DesiredDeploymentState.STOPPED)) {
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.Success _ ->
                    switch (stopContainerPort.stop(command.serviceName())) {
                        case StopContainerPort.StopContainerResult.Success _ -> new StopServiceResult.Success();
                        case StopContainerPort.StopContainerResult.MissingContainer _ -> new StopServiceResult.NotDeployed();
                        case StopContainerPort.StopContainerResult.DuplicateManagedContainers _ -> new StopServiceResult.Drift();
                        case StopContainerPort.StopContainerResult.Failure _ -> new StopServiceResult.DockerFailure();
                    };
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.ServiceNotFound _ ->
                    new StopServiceResult.ServiceNotFound();
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.NotDeployed _ ->
                    new StopServiceResult.NotDeployed();
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.Failure _ ->
                    new StopServiceResult.DesiredStateFailure();
        };
    }

    @Override
    public UninstallServiceResult uninstallService(UninstallServiceCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (findDesiredDeploymentStatePort.findDesiredState(command.serviceName())) {
            case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Found _ ->
                    uninstallDeployedService(command.serviceName());
            case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.NotDeployed _ ->
                    uninstallWithoutDesiredDeployment(command.serviceName());
            case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.ServiceNotFound _ ->
                    new UninstallServiceResult.ServiceNotFound();
            case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Failure _ ->
                    new UninstallServiceResult.DesiredStateFailure();
        };
    }

    @Override
    public GetServiceRuntimeStatusResult getServiceRuntimeStatus(GetServiceRuntimeStatusCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (findDesiredDeploymentStatePort.findDesiredState(command.serviceName())) {
            case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Found found ->
                    switch (findStatus(command.serviceName(), Optional.of(found.desiredState()))) {
                        case RuntimeStatusResult.Success success ->
                                new GetServiceRuntimeStatusResult.Success(success.status());
                        case RuntimeStatusResult.DockerFailure _ -> new GetServiceRuntimeStatusResult.DockerFailure();
                    };
            case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.NotDeployed _ ->
                    switch (findStatus(command.serviceName(), Optional.empty())) {
                        case RuntimeStatusResult.Success success ->
                                new GetServiceRuntimeStatusResult.Success(success.status());
                        case RuntimeStatusResult.DockerFailure _ -> new GetServiceRuntimeStatusResult.DockerFailure();
                    };
            case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.ServiceNotFound _ ->
                    new GetServiceRuntimeStatusResult.ServiceNotFound();
            case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Failure _ ->
                    new GetServiceRuntimeStatusResult.DesiredStateFailure();
        };
    }

    @Override
    public GetServiceSummariesResult getServiceSummaries() {
        return switch (findServiceSummaryCandidatesPort.findServiceSummaryCandidates()) {
            case FindServiceSummaryCandidatesPort.FindServiceSummaryCandidatesResult.Found found ->
                    serviceSummariesFrom(found.services());
            case FindServiceSummaryCandidatesPort.FindServiceSummaryCandidatesResult.Failure _ ->
                    new GetServiceSummariesResult.Failure();
        };
    }

    private UninstallServiceResult uninstallDeployedService(ServiceName serviceName) {
        return switch (removeContainerPort.remove(serviceName)) {
            case RemoveContainerPort.RemoveContainerResult.Success _,
                 RemoveContainerPort.RemoveContainerResult.MissingContainer _ -> deleteDesiredDeployment(serviceName);
            case RemoveContainerPort.RemoveContainerResult.DuplicateManagedContainers _ ->
                    new UninstallServiceResult.Drift();
            case RemoveContainerPort.RemoveContainerResult.Failure _ -> new UninstallServiceResult.DockerFailure();
        };
    }

    private UninstallServiceResult uninstallWithoutDesiredDeployment(ServiceName serviceName) {
        return switch (removeContainerPort.remove(serviceName)) {
            case RemoveContainerPort.RemoveContainerResult.Success _ -> new UninstallServiceResult.Success();
            case RemoveContainerPort.RemoveContainerResult.MissingContainer _ -> new UninstallServiceResult.NotDeployed();
            case RemoveContainerPort.RemoveContainerResult.DuplicateManagedContainers _ ->
                    new UninstallServiceResult.Drift();
            case RemoveContainerPort.RemoveContainerResult.Failure _ -> new UninstallServiceResult.DockerFailure();
        };
    }

    private UninstallServiceResult deleteDesiredDeployment(ServiceName serviceName) {
        return switch (deleteDesiredDeploymentPort.delete(serviceName)) {
            case DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult.Deleted _ ->
                    new UninstallServiceResult.Success();
            case DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult.ServiceNotFound _ ->
                    new UninstallServiceResult.ServiceNotFound();
            case DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult.NotDeployed _ ->
                    new UninstallServiceResult.NotDeployed();
            case DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult.Failure _ ->
                    new UninstallServiceResult.DesiredStateFailure();
        };
    }

    private DeployServiceResult deployFoundService(Service service, ImageVersion imageVersion) {
        DeploymentId deploymentId;
        switch (recordDeploymentHistoryPort.recordDeployment(service.name(), imageVersion)) {
            case RecordDeploymentHistoryPort.RecordDeploymentHistoryResult.Recorded recorded -> {
                deploymentId = recorded.deploymentId();
            }
            case RecordDeploymentHistoryPort.RecordDeploymentHistoryResult.ServiceNotFound _ -> {
                return new DeployServiceResult.ServiceNotFound();
            }
            case RecordDeploymentHistoryPort.RecordDeploymentHistoryResult.Failure _ -> {
                return new DeployServiceResult.DesiredStateFailure();
            }
        }

        DesiredDeployment desiredDeployment = new DesiredDeployment(
                service.name(),
                service.imageRepository(),
                imageVersion,
                service.runtimeConfiguration(),
                DesiredDeploymentState.RUNNING
        );

        return switch (upsertDesiredDeploymentPort.upsert(desiredDeployment)) {
            case UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success _ ->
                    switch (deployContainerPort.deploy(desiredDeployment, deploymentId)) {
                        case DeployContainerPort.DeployContainerResult.Success _ -> new DeployServiceResult.Success();
                        case DeployContainerPort.DeployContainerResult.Failure _ ->
                                new DeployServiceResult.DockerFailure();
                    };
            case UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.ServiceNotFound _ ->
                    new DeployServiceResult.ServiceNotFound();
            case UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Failure _ ->
                    new DeployServiceResult.DesiredStateFailure();
        };
    }

    private GetServiceSummariesResult serviceSummariesFrom(
            List<FindServiceSummaryCandidatesPort.ServiceSummaryCandidate> candidates
    ) {
        List<ServiceSummary> serviceSummaries = new ArrayList<>();
        for (FindServiceSummaryCandidatesPort.ServiceSummaryCandidate candidate : candidates) {
            switch (findStatus(candidate.name(), candidate.desiredState())) {
                case RuntimeStatusResult.Success success -> serviceSummaries.add(new ServiceSummary(
                        candidate.name(),
                        candidate.imageRepository(),
                        candidate.deployedVersion(),
                        success.status()
                ));
                case RuntimeStatusResult.DockerFailure _ -> {
                    return new GetServiceSummariesResult.Failure();
                }
            }
        }
        return new GetServiceSummariesResult.Success(List.copyOf(serviceSummaries));
    }

    private RuntimeStatusResult findStatus(
            ServiceName serviceName,
            Optional<DesiredDeploymentState> desiredState
    ) {
        return switch (findActualDeploymentStatePort.findActualState(serviceName)) {
            case FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found found ->
                    new RuntimeStatusResult.Success(desiredState
                            .map(state -> statusFor(state, found.actualState()))
                            .orElseGet(() -> statusWithoutDesiredDeployment(found.actualState())));
            case FindActualDeploymentStatePort.FindActualDeploymentStateResult.DuplicateManagedContainers _ ->
                    new RuntimeStatusResult.Success(ServiceRuntimeStatus.DUPLICATE_MANAGED_CONTAINERS);
            case FindActualDeploymentStatePort.FindActualDeploymentStateResult.Failure _ ->
                    new RuntimeStatusResult.DockerFailure();
        };
    }

    private sealed interface RuntimeStatusResult permits RuntimeStatusResult.Success, RuntimeStatusResult.DockerFailure {

        record Success(ServiceRuntimeStatus status) implements RuntimeStatusResult {
        }

        record DockerFailure() implements RuntimeStatusResult {
        }
    }

    private static ServiceRuntimeStatus statusFor(
            DesiredDeploymentState desiredState,
            ActualDeploymentState actualState
    ) {
        return switch (desiredState) {
            case RUNNING -> switch (actualState) {
                case RUNNING -> ServiceRuntimeStatus.RUNNING;
                case STOPPED -> ServiceRuntimeStatus.EXPECTED_RUNNING_BUT_STOPPED;
                case MISSING -> ServiceRuntimeStatus.EXPECTED_RUNNING_BUT_MISSING;
            };
            case STOPPED -> switch (actualState) {
                case RUNNING -> ServiceRuntimeStatus.EXPECTED_STOPPED_BUT_RUNNING;
                case STOPPED -> ServiceRuntimeStatus.STOPPED;
                case MISSING -> ServiceRuntimeStatus.EXPECTED_STOPPED_BUT_MISSING;
            };
        };
    }

    private static ServiceRuntimeStatus statusWithoutDesiredDeployment(ActualDeploymentState actualState) {
        return switch (actualState) {
            case RUNNING -> ServiceRuntimeStatus.ORPHANED_RUNNING;
            case STOPPED -> ServiceRuntimeStatus.ORPHANED_STOPPED;
            case MISSING -> ServiceRuntimeStatus.NOT_DEPLOYED;
        };
    }
}
