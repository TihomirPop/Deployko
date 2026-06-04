package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.port.in.CreateServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServicePortMappingsUseCase;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.FindServicePortMappingsPort;
import java.util.Objects;

public final class ServiceRuntimeConfigurationDomainService
        implements GetServicePortMappingsUseCase, CreateServicePortMappingUseCase, DeleteServicePortMappingUseCase {

    private final FindServicePortMappingsPort findServicePortMappingsPort;
    private final CreateServicePortMappingPort createServicePortMappingPort;
    private final DeleteServicePortMappingPort deleteServicePortMappingPort;

    public ServiceRuntimeConfigurationDomainService(
            FindServicePortMappingsPort findServicePortMappingsPort,
            CreateServicePortMappingPort createServicePortMappingPort,
            DeleteServicePortMappingPort deleteServicePortMappingPort
    ) {
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
}
