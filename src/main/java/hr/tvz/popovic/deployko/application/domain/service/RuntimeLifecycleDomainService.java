package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.StartServiceUseCase.StartServiceCommand;
import hr.tvz.popovic.deployko.application.port.in.StartServiceUseCase.StartServiceResult;
import hr.tvz.popovic.deployko.application.port.in.StopServiceUseCase.StopServiceCommand;
import hr.tvz.popovic.deployko.application.port.in.StopServiceUseCase.StopServiceResult;
import hr.tvz.popovic.deployko.application.port.in.UninstallServiceUseCase.UninstallServiceCommand;
import hr.tvz.popovic.deployko.application.port.in.UninstallServiceUseCase.UninstallServiceResult;
import hr.tvz.popovic.deployko.application.port.out.DeleteDesiredDeploymentPort;
import hr.tvz.popovic.deployko.application.port.out.FindDesiredDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.RemoveContainerPort;
import hr.tvz.popovic.deployko.application.port.out.StartContainerPort;
import hr.tvz.popovic.deployko.application.port.out.StopContainerPort;
import hr.tvz.popovic.deployko.application.port.out.UpdateDesiredDeploymentStatePort;

import java.util.Objects;

public final class RuntimeLifecycleDomainService {

    private final UpdateDesiredDeploymentStatePort updateDesiredDeploymentStatePort;
    private final FindDesiredDeploymentStatePort findDesiredDeploymentStatePort;
    private final StartContainerPort startContainerPort;
    private final StopContainerPort stopContainerPort;
    private final RemoveContainerPort removeContainerPort;
    private final DeleteDesiredDeploymentPort deleteDesiredDeploymentPort;

    public RuntimeLifecycleDomainService(
            UpdateDesiredDeploymentStatePort updateDesiredDeploymentStatePort,
            FindDesiredDeploymentStatePort findDesiredDeploymentStatePort,
            StartContainerPort startContainerPort,
            StopContainerPort stopContainerPort,
            RemoveContainerPort removeContainerPort,
            DeleteDesiredDeploymentPort deleteDesiredDeploymentPort
    ) {
        this.updateDesiredDeploymentStatePort = Objects.requireNonNull(
                updateDesiredDeploymentStatePort,
                "updateDesiredDeploymentStatePort must not be null"
        );
        this.findDesiredDeploymentStatePort = Objects.requireNonNull(
                findDesiredDeploymentStatePort,
                "findDesiredDeploymentStatePort must not be null"
        );
        this.startContainerPort = Objects.requireNonNull(startContainerPort, "startContainerPort must not be null");
        this.stopContainerPort = Objects.requireNonNull(stopContainerPort, "stopContainerPort must not be null");
        this.removeContainerPort = Objects.requireNonNull(removeContainerPort, "removeContainerPort must not be null");
        this.deleteDesiredDeploymentPort = Objects.requireNonNull(
                deleteDesiredDeploymentPort,
                "deleteDesiredDeploymentPort must not be null"
        );
    }

    public StartServiceResult startService(StartServiceCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (updateDesiredDeploymentStatePort.updateState(command.serviceName(), DesiredDeploymentState.RUNNING)) {
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.Success _ ->
                    switch (startContainerPort.start(command.serviceName())) {
                        case StartContainerPort.StartContainerResult.Success _ -> new StartServiceResult.Success();
                        case StartContainerPort.StartContainerResult.MissingContainer _ ->
                                new StartServiceResult.NotDeployed();
                        case StartContainerPort.StartContainerResult.DuplicateManagedContainers _ ->
                                new StartServiceResult.Drift();
                        case StartContainerPort.StartContainerResult.Failure _ ->
                                new StartServiceResult.DockerFailure();
                    };
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.ServiceNotFound _ ->
                    new StartServiceResult.ServiceNotFound();
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.NotDeployed _ ->
                    new StartServiceResult.NotDeployed();
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.Failure _ ->
                    new StartServiceResult.DesiredStateFailure();
        };
    }

    public StopServiceResult stopService(StopServiceCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (updateDesiredDeploymentStatePort.updateState(command.serviceName(), DesiredDeploymentState.STOPPED)) {
            case UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.Success _ ->
                    switch (stopContainerPort.stop(command.serviceName())) {
                        case StopContainerPort.StopContainerResult.Success _ -> new StopServiceResult.Success();
                        case StopContainerPort.StopContainerResult.MissingContainer _ ->
                                new StopServiceResult.NotDeployed();
                        case StopContainerPort.StopContainerResult.DuplicateManagedContainers _ ->
                                new StopServiceResult.Drift();
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
            case RemoveContainerPort.RemoveContainerResult.MissingContainer _ ->
                    new UninstallServiceResult.NotDeployed();
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
}
