package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.RuntimeConfiguration;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.port.in.ServiceManagementUseCase;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceByNamePort;
import java.util.Objects;

public class ServiceManagementDomainService implements ServiceManagementUseCase {

    private final CreateServicePort createServicePort;
    private final DeleteServiceByNamePort deleteServiceByNamePort;

    public ServiceManagementDomainService(
            CreateServicePort createServicePort,
            DeleteServiceByNamePort deleteServiceByNamePort
    ) {
        this.createServicePort = Objects.requireNonNull(createServicePort, "createServicePort must not be null");
        this.deleteServiceByNamePort = Objects.requireNonNull(
                deleteServiceByNamePort,
                "deleteServiceByNamePort must not be null"
        );
    }

    @Override
    public CreateServiceResult createService(CreateServiceCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return createNewService(command);
    }

    @Override
    public DeleteServiceResult deleteService(DeleteServiceCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (deleteServiceByNamePort.deleteByName(command.serviceName())) {
            case DeleteServiceByNamePort.DeleteServiceByNameResult.Deleted _ -> new DeleteServiceResult.Success();
            case DeleteServiceByNamePort.DeleteServiceByNameResult.NotFound _ -> new DeleteServiceResult.NotFound();
            case DeleteServiceByNamePort.DeleteServiceByNameResult.Failure _ -> new DeleteServiceResult.Failure();
        };
    }

    private CreateServiceResult createNewService(CreateServiceCommand command) {
        Service service = new Service(
                command.serviceName(),
                command.imageRepository(),
                RuntimeConfiguration.empty()
        );

        return switch (createServicePort.create(service)) {
            case CreateServicePort.CreateServicePortResult.Success _ -> new CreateServiceResult.Success(service);
            case CreateServicePort.CreateServicePortResult.AlreadyExists _ -> new CreateServiceResult.DuplicateServiceName();
            case CreateServicePort.CreateServicePortResult.Failure _ -> new CreateServiceResult.Failure();
        };
    }
}
