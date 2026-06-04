package hr.tvz.popovic.deployko.adapter.in;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.CreateServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceNamesUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/services")
public class ServiceController {

    private final CreateServiceUseCase createServiceUseCase;
    private final DeleteServiceUseCase deleteServiceUseCase;
    private final GetServiceNamesUseCase getServiceNamesUseCase;

    public ServiceController(
            CreateServiceUseCase createServiceUseCase,
            DeleteServiceUseCase deleteServiceUseCase,
            GetServiceNamesUseCase getServiceNamesUseCase
    ) {
        this.createServiceUseCase = createServiceUseCase;
        this.deleteServiceUseCase = deleteServiceUseCase;
        this.getServiceNamesUseCase = getServiceNamesUseCase;
    }

    @GetMapping
    public ResponseEntity<?> getServiceNames() {
        return switch (getServiceNamesUseCase.getServiceNames()) {
            case GetServiceNamesUseCase.GetServiceNamesResult.Success success ->
                    ResponseEntity.ok(ServiceNamesHttpResponse.from(success.serviceNames()));
            case GetServiceNamesUseCase.GetServiceNamesResult.Failure _ -> ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        };
    }

    @PostMapping
    public ResponseEntity<?> createService(@RequestBody CreateServiceHttpRequest request) {
        try {
            CreateServiceUseCase.CreateServiceResult result = createServiceUseCase.createService(
                    new CreateServiceUseCase.CreateServiceCommand(
                            new ServiceName(request.name()),
                            new ImageRepository(request.imageRepository())
                    )
            );

            return switch (result) {
                case CreateServiceUseCase.CreateServiceResult.Success success -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(CreateServiceHttpResponse.from(success.service()));
                case CreateServiceUseCase.CreateServiceResult.DuplicateServiceName _ -> ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .build();
                case CreateServiceUseCase.CreateServiceResult.Failure _ -> ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{serviceName}")
    public ResponseEntity<Void> deleteService(@PathVariable String serviceName) {
        try {
            DeleteServiceUseCase.DeleteServiceResult result = deleteServiceUseCase.deleteService(
                    new DeleteServiceUseCase.DeleteServiceCommand(new ServiceName(serviceName))
            );

            return switch (result) {
                case DeleteServiceUseCase.DeleteServiceResult.Success _ -> ResponseEntity.noContent().build();
                case DeleteServiceUseCase.DeleteServiceResult.NotFound _ -> ResponseEntity.notFound().build();
                case DeleteServiceUseCase.DeleteServiceResult.Failure _ -> ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
    }

    public record CreateServiceHttpRequest(String name, String imageRepository) {
    }

    public record CreateServiceHttpResponse(String name, String imageRepository) {

        static CreateServiceHttpResponse from(Service service) {
            return new CreateServiceHttpResponse(
                    service.name().value(),
                    service.imageRepository().value()
            );
        }
    }

    public record ServiceNamesHttpResponse(List<String> serviceNames) {

        static ServiceNamesHttpResponse from(List<ServiceName> serviceNames) {
            return new ServiceNamesHttpResponse(serviceNames
                    .stream()
                    .map(ServiceName::value)
                    .toList());
        }
    }
}
