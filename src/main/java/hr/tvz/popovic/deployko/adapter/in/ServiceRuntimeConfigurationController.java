package hr.tvz.popovic.deployko.adapter.in;

import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.PortMappings;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.GetServicePortMappingsUseCase;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/services/{serviceName}/runtime-configuration")
public class ServiceRuntimeConfigurationController {

    private final GetServicePortMappingsUseCase getServicePortMappingsUseCase;

    public ServiceRuntimeConfigurationController(GetServicePortMappingsUseCase getServicePortMappingsUseCase) {
        this.getServicePortMappingsUseCase = getServicePortMappingsUseCase;
    }

    @GetMapping("/port-mappings")
    public ResponseEntity<?> getPortMappings(@PathVariable String serviceName) {
        try {
            GetServicePortMappingsUseCase.GetServicePortMappingsResult result =
                    getServicePortMappingsUseCase.getServicePortMappings(
                            new GetServicePortMappingsUseCase.GetServicePortMappingsCommand(
                                    new ServiceName(serviceName)
                            )
                    );

            return switch (result) {
                case GetServicePortMappingsUseCase.GetServicePortMappingsResult.Success success ->
                        ResponseEntity.ok(PortMappingHttpResponse.from(success.portMappings()));
                case GetServicePortMappingsUseCase.GetServicePortMappingsResult.NotFound _ ->
                        ResponseEntity.notFound().build();
                case GetServicePortMappingsUseCase.GetServicePortMappingsResult.Failure _ ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
    }

    public record PortMappingHttpResponse(
            int hostPort,
            String hostProtocol,
            int containerPort,
            String containerProtocol
    ) {

        static List<PortMappingHttpResponse> from(PortMappings portMappings) {
            return portMappings
                    .asMap()
                    .entrySet()
                    .stream()
                    .map(entry -> from(entry.getKey(), entry.getValue()))
                    .toList();
        }

        private static PortMappingHttpResponse from(Port hostPort, Port containerPort) {
            return new PortMappingHttpResponse(
                    hostPort.value(),
                    hostPort.protocol().name(),
                    containerPort.value(),
                    containerPort.protocol().name()
            );
        }
    }
}
