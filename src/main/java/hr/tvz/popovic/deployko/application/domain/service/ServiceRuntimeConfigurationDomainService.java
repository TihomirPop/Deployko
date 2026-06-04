package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.port.in.CreateServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.CreateServiceVolumeMountUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServiceVolumeMountUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceEnvironmentVariablesUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServicePortMappingsUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceVolumeMountsUseCase;
import hr.tvz.popovic.deployko.application.port.in.UpdateServiceVolumeMountUseCase;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.CreateServiceVolumeMountPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceVolumeMountPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceEnvironmentVariablesPort;
import hr.tvz.popovic.deployko.application.port.out.FindServicePortMappingsPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceVolumeMountsPort;
import hr.tvz.popovic.deployko.application.port.out.UpdateServiceVolumeMountPort;
import java.util.Objects;

public final class ServiceRuntimeConfigurationDomainService
        implements GetServiceEnvironmentVariablesUseCase, GetServicePortMappingsUseCase, CreateServicePortMappingUseCase,
        DeleteServicePortMappingUseCase, GetServiceVolumeMountsUseCase, CreateServiceVolumeMountUseCase, UpdateServiceVolumeMountUseCase,
        DeleteServiceVolumeMountUseCase {

    private final FindServiceEnvironmentVariablesPort findServiceEnvironmentVariablesPort;
    private final FindServicePortMappingsPort findServicePortMappingsPort;
    private final CreateServicePortMappingPort createServicePortMappingPort;
    private final DeleteServicePortMappingPort deleteServicePortMappingPort;
    private final FindServiceVolumeMountsPort findServiceVolumeMountsPort;
    private final CreateServiceVolumeMountPort createServiceVolumeMountPort;
    private final UpdateServiceVolumeMountPort updateServiceVolumeMountPort;
    private final DeleteServiceVolumeMountPort deleteServiceVolumeMountPort;

    public ServiceRuntimeConfigurationDomainService(
            FindServiceEnvironmentVariablesPort findServiceEnvironmentVariablesPort,
            FindServicePortMappingsPort findServicePortMappingsPort,
            CreateServicePortMappingPort createServicePortMappingPort,
            DeleteServicePortMappingPort deleteServicePortMappingPort,
            FindServiceVolumeMountsPort findServiceVolumeMountsPort,
            CreateServiceVolumeMountPort createServiceVolumeMountPort,
            UpdateServiceVolumeMountPort updateServiceVolumeMountPort,
            DeleteServiceVolumeMountPort deleteServiceVolumeMountPort
    ) {
        this.findServiceEnvironmentVariablesPort = Objects.requireNonNull(
                findServiceEnvironmentVariablesPort,
                "findServiceEnvironmentVariablesPort must not be null"
        );
        this.findServicePortMappingsPort = Objects.requireNonNull(
                findServicePortMappingsPort,
                "findServicePortMappingsPort must not be null"
        );
        this.createServicePortMappingPort = Objects.requireNonNull(
                createServicePortMappingPort,
                "createServicePortMappingPort must not be null"
        );
        this.deleteServicePortMappingPort = Objects.requireNonNull(
                deleteServicePortMappingPort,
                "deleteServicePortMappingPort must not be null"
        );
        this.findServiceVolumeMountsPort = Objects.requireNonNull(
                findServiceVolumeMountsPort,
                "findServiceVolumeMountsPort must not be null"
        );
        this.createServiceVolumeMountPort = Objects.requireNonNull(
                createServiceVolumeMountPort,
                "createServiceVolumeMountPort must not be null"
        );
        this.updateServiceVolumeMountPort = Objects.requireNonNull(
                updateServiceVolumeMountPort,
                "updateServiceVolumeMountPort must not be null"
        );
        this.deleteServiceVolumeMountPort = Objects.requireNonNull(
                deleteServiceVolumeMountPort,
                "deleteServiceVolumeMountPort must not be null"
        );
    }

    @Override
    public GetServiceEnvironmentVariablesResult getServiceEnvironmentVariables(
            GetServiceEnvironmentVariablesCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (findServiceEnvironmentVariablesPort.findEnvironmentVariables(command.serviceName())) {
            case FindServiceEnvironmentVariablesPort.FindServiceEnvironmentVariablesResult.Found found ->
                    new GetServiceEnvironmentVariablesResult.Success(found.environmentVariables());
            case FindServiceEnvironmentVariablesPort.FindServiceEnvironmentVariablesResult.ServiceNotFound _ ->
                    new GetServiceEnvironmentVariablesResult.NotFound();
            case FindServiceEnvironmentVariablesPort.FindServiceEnvironmentVariablesResult.Failure _ ->
                    new GetServiceEnvironmentVariablesResult.Failure();
        };
    }

    @Override
    public GetServicePortMappingsResult getServicePortMappings(GetServicePortMappingsCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (findServicePortMappingsPort.findPortMappings(command.serviceName())) {
            case FindServicePortMappingsPort.FindServicePortMappingsResult.Found found ->
                    new GetServicePortMappingsResult.Success(found.portMappings());
            case FindServicePortMappingsPort.FindServicePortMappingsResult.ServiceNotFound _ ->
                    new GetServicePortMappingsResult.NotFound();
            case FindServicePortMappingsPort.FindServicePortMappingsResult.Failure _ ->
                    new GetServicePortMappingsResult.Failure();
        };
    }

    @Override
    public CreateServicePortMappingResult createServicePortMapping(CreateServicePortMappingCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (createServicePortMappingPort.createPortMapping(
                command.serviceName(),
                command.hostPort(),
                command.containerPort()
        )) {
            case CreateServicePortMappingPort.CreateServicePortMappingResult.Created _ ->
                    new CreateServicePortMappingResult.Success();
            case CreateServicePortMappingPort.CreateServicePortMappingResult.ServiceNotFound _ ->
                    new CreateServicePortMappingResult.ServiceNotFound();
            case CreateServicePortMappingPort.CreateServicePortMappingResult.AlreadyExists _ ->
                    new CreateServicePortMappingResult.AlreadyExists();
            case CreateServicePortMappingPort.CreateServicePortMappingResult.Failure _ ->
                    new CreateServicePortMappingResult.Failure();
        };
    }

    @Override
    public DeleteServicePortMappingResult deleteServicePortMapping(DeleteServicePortMappingCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (deleteServicePortMappingPort.deletePortMapping(command.serviceName(), command.hostPort())) {
            case DeleteServicePortMappingPort.DeleteServicePortMappingResult.Deleted _ ->
                    new DeleteServicePortMappingResult.Success();
            case DeleteServicePortMappingPort.DeleteServicePortMappingResult.ServiceNotFound _ ->
                    new DeleteServicePortMappingResult.ServiceNotFound();
            case DeleteServicePortMappingPort.DeleteServicePortMappingResult.PortMappingNotFound _ ->
                    new DeleteServicePortMappingResult.PortMappingNotFound();
            case DeleteServicePortMappingPort.DeleteServicePortMappingResult.Failure _ ->
                    new DeleteServicePortMappingResult.Failure();
        };
    }

    @Override
    public GetServiceVolumeMountsResult getServiceVolumeMounts(GetServiceVolumeMountsCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (findServiceVolumeMountsPort.findVolumeMounts(command.serviceName())) {
            case FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.Found found ->
                    new GetServiceVolumeMountsResult.Success(found.volumeMounts());
            case FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.ServiceNotFound _ ->
                    new GetServiceVolumeMountsResult.NotFound();
            case FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.Failure _ ->
                    new GetServiceVolumeMountsResult.Failure();
        };
    }

    @Override
    public CreateServiceVolumeMountResult createServiceVolumeMount(CreateServiceVolumeMountCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (createServiceVolumeMountPort.createVolumeMount(command.serviceName(), command.volumeMount())) {
            case CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.Created _ ->
                    new CreateServiceVolumeMountResult.Success();
            case CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.ServiceNotFound _ ->
                    new CreateServiceVolumeMountResult.ServiceNotFound();
            case CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.AlreadyExists _ ->
                    new CreateServiceVolumeMountResult.AlreadyExists();
            case CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.Failure _ ->
                    new CreateServiceVolumeMountResult.Failure();
        };
    }

    @Override
    public UpdateServiceVolumeMountResult updateServiceVolumeMount(UpdateServiceVolumeMountCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (updateServiceVolumeMountPort.updateVolumeMount(command.serviceName(), command.volumeMount())) {
            case UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.Updated _ ->
                    new UpdateServiceVolumeMountResult.Success();
            case UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.ServiceNotFound _ ->
                    new UpdateServiceVolumeMountResult.ServiceNotFound();
            case UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.VolumeMountNotFound _ ->
                    new UpdateServiceVolumeMountResult.VolumeMountNotFound();
            case UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.Failure _ ->
                    new UpdateServiceVolumeMountResult.Failure();
        };
    }

    @Override
    public DeleteServiceVolumeMountResult deleteServiceVolumeMount(DeleteServiceVolumeMountCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (deleteServiceVolumeMountPort.deleteVolumeMount(command.serviceName(), command.target())) {
            case DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.Deleted _ ->
                    new DeleteServiceVolumeMountResult.Success();
            case DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.ServiceNotFound _ ->
                    new DeleteServiceVolumeMountResult.ServiceNotFound();
            case DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.VolumeMountNotFound _ ->
                    new DeleteServiceVolumeMountResult.VolumeMountNotFound();
            case DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.Failure _ ->
                    new DeleteServiceVolumeMountResult.Failure();
        };
    }
}
