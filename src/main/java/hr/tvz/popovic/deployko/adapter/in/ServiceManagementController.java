package hr.tvz.popovic.deployko.adapter.in;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.ServiceManagementUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/services")
public class ServiceManagementController {

    private final ServiceManagementUseCase serviceManagementUseCase;

    public ServiceManagementController(ServiceManagementUseCase serviceManagementUseCase) {
        this.serviceManagementUseCase = serviceManagementUseCase;
    }

    @PostMapping
    public ResponseEntity<?> createService(@RequestBody CreateServiceHttpRequest request) {
        try {
            ServiceManagementUseCase.CreateServiceResult result = serviceManagementUseCase.createService(
                    new ServiceManagementUseCase.CreateServiceCommand(
                            new ServiceName(request.name()),
                            new ImageRepository(request.imageRepository())
                    )
            );

            return switch (result) {
                case ServiceManagementUseCase.CreateServiceResult.Success success -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(CreateServiceHttpResponse.from(success.service()));
                case ServiceManagementUseCase.CreateServiceResult.DuplicateServiceName _ -> ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .build();
                case ServiceManagementUseCase.CreateServiceResult.Failure _ -> ResponseEntity
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
            ServiceManagementUseCase.DeleteServiceResult result = serviceManagementUseCase.deleteService(
                    new ServiceManagementUseCase.DeleteServiceCommand(new ServiceName(serviceName))
            );

            return switch (result) {
                case ServiceManagementUseCase.DeleteServiceResult.Success _ -> ResponseEntity.noContent().build();
                case ServiceManagementUseCase.DeleteServiceResult.NotFound _ -> ResponseEntity.notFound().build();
                case ServiceManagementUseCase.DeleteServiceResult.Failure _ -> ResponseEntity
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
}
