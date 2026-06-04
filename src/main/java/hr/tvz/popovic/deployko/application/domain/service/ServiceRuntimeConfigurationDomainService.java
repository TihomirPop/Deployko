package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.port.in.GetServicePortMappingsUseCase;
import hr.tvz.popovic.deployko.application.port.out.FindServicePortMappingsPort;
import java.util.Objects;

public final class ServiceRuntimeConfigurationDomainService implements GetServicePortMappingsUseCase {

    private final FindServicePortMappingsPort findServicePortMappingsPort;

    public ServiceRuntimeConfigurationDomainService(FindServicePortMappingsPort findServicePortMappingsPort) {
        this.findServicePortMappingsPort = Objects.requireNonNull(
                findServicePortMappingsPort,
                "findServicePortMappingsPort must not be null"
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
}
