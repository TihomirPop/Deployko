package hr.tvz.popovic.deployko.adapter.in;

import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.GetServiceVersionsUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/services/{serviceName}/versions")
public class ServiceVersionController {

    private final GetServiceVersionsUseCase getServiceVersionsUseCase;

    public ServiceVersionController(GetServiceVersionsUseCase getServiceVersionsUseCase) {
        this.getServiceVersionsUseCase = getServiceVersionsUseCase;
    }

    @GetMapping
    public ResponseEntity<?> getServiceVersions(@PathVariable String serviceName) {
        try {
            GetServiceVersionsUseCase.GetServiceVersionsResult result = getServiceVersionsUseCase.getServiceVersions(
                    new GetServiceVersionsUseCase.GetServiceVersionsCommand(new ServiceName(serviceName))
            );

            return switch (result) {
                case GetServiceVersionsUseCase.GetServiceVersionsResult.Success success ->
                        ResponseEntity.ok(ServiceVersionsHttpResponse.from(success.imageVersions()));
                case GetServiceVersionsUseCase.GetServiceVersionsResult.NotFound _ -> ResponseEntity.notFound().build();
                case GetServiceVersionsUseCase.GetServiceVersionsResult.Failure _ -> ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
    }

    public record ServiceVersionsHttpResponse(List<String> imageVersions) {

        static ServiceVersionsHttpResponse from(List<ImageVersion> imageVersions) {
            return new ServiceVersionsHttpResponse(imageVersions
                    .stream()
                    .map(ImageVersion::value)
                    .toList());
        }
    }
}
