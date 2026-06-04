package hr.tvz.popovic.deployko.adapter.in;

import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.PortMappings;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMounts;
import hr.tvz.popovic.deployko.application.port.in.CreateServiceEnvironmentVariableUseCase;
import hr.tvz.popovic.deployko.application.port.in.CreateServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.CreateServiceVolumeMountUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServiceEnvironmentVariableUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServicePortMappingUseCase;
import hr.tvz.popovic.deployko.application.port.in.DeleteServiceVolumeMountUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceEnvironmentVariablesUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServicePortMappingsUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetServiceVolumeMountsUseCase;
import hr.tvz.popovic.deployko.application.port.in.UpdateServiceEnvironmentVariableUseCase;
import hr.tvz.popovic.deployko.application.port.in.UpdateServiceVolumeMountUseCase;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/services/{serviceName}/runtime-configuration")
public class ServiceRuntimeConfigurationController {

    private final GetServicePortMappingsUseCase getServicePortMappingsUseCase;
    private final GetServiceEnvironmentVariablesUseCase getServiceEnvironmentVariablesUseCase;
    private final CreateServiceEnvironmentVariableUseCase createServiceEnvironmentVariableUseCase;
    private final UpdateServiceEnvironmentVariableUseCase updateServiceEnvironmentVariableUseCase;
    private final DeleteServiceEnvironmentVariableUseCase deleteServiceEnvironmentVariableUseCase;
    private final CreateServicePortMappingUseCase createServicePortMappingUseCase;
    private final DeleteServicePortMappingUseCase deleteServicePortMappingUseCase;
    private final GetServiceVolumeMountsUseCase getServiceVolumeMountsUseCase;
    private final CreateServiceVolumeMountUseCase createServiceVolumeMountUseCase;
    private final UpdateServiceVolumeMountUseCase updateServiceVolumeMountUseCase;
    private final DeleteServiceVolumeMountUseCase deleteServiceVolumeMountUseCase;

    public ServiceRuntimeConfigurationController(
            GetServiceEnvironmentVariablesUseCase getServiceEnvironmentVariablesUseCase,
            CreateServiceEnvironmentVariableUseCase createServiceEnvironmentVariableUseCase,
            UpdateServiceEnvironmentVariableUseCase updateServiceEnvironmentVariableUseCase,
            DeleteServiceEnvironmentVariableUseCase deleteServiceEnvironmentVariableUseCase,
            GetServicePortMappingsUseCase getServicePortMappingsUseCase,
            CreateServicePortMappingUseCase createServicePortMappingUseCase,
            DeleteServicePortMappingUseCase deleteServicePortMappingUseCase,
            GetServiceVolumeMountsUseCase getServiceVolumeMountsUseCase,
            CreateServiceVolumeMountUseCase createServiceVolumeMountUseCase,
            UpdateServiceVolumeMountUseCase updateServiceVolumeMountUseCase,
            DeleteServiceVolumeMountUseCase deleteServiceVolumeMountUseCase
    ) {
        this.getServiceEnvironmentVariablesUseCase = getServiceEnvironmentVariablesUseCase;
        this.createServiceEnvironmentVariableUseCase = createServiceEnvironmentVariableUseCase;
        this.updateServiceEnvironmentVariableUseCase = updateServiceEnvironmentVariableUseCase;
        this.deleteServiceEnvironmentVariableUseCase = deleteServiceEnvironmentVariableUseCase;
        this.getServicePortMappingsUseCase = getServicePortMappingsUseCase;
        this.createServicePortMappingUseCase = createServicePortMappingUseCase;
        this.deleteServicePortMappingUseCase = deleteServicePortMappingUseCase;
        this.getServiceVolumeMountsUseCase = getServiceVolumeMountsUseCase;
        this.createServiceVolumeMountUseCase = createServiceVolumeMountUseCase;
        this.updateServiceVolumeMountUseCase = updateServiceVolumeMountUseCase;
        this.deleteServiceVolumeMountUseCase = deleteServiceVolumeMountUseCase;
    }

    @GetMapping("/environment-variables")
    public ResponseEntity<?> getEnvironmentVariables(@PathVariable String serviceName) {
        try {
            GetServiceEnvironmentVariablesUseCase.GetServiceEnvironmentVariablesResult result =
                    getServiceEnvironmentVariablesUseCase.getServiceEnvironmentVariables(
                            new GetServiceEnvironmentVariablesUseCase.GetServiceEnvironmentVariablesCommand(
                                    new ServiceName(serviceName)
                            )
                    );

            return switch (result) {
                case GetServiceEnvironmentVariablesUseCase.GetServiceEnvironmentVariablesResult.Success success ->
                        ResponseEntity.ok(EnvironmentVariableHttpResponse.from(success.environmentVariables()));
                case GetServiceEnvironmentVariablesUseCase.GetServiceEnvironmentVariablesResult.NotFound _ ->
                        ResponseEntity.notFound().build();
                case GetServiceEnvironmentVariablesUseCase.GetServiceEnvironmentVariablesResult.Failure _ ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/environment-variables")
    public ResponseEntity<Void> createEnvironmentVariable(
            @PathVariable String serviceName,
            @RequestBody CreateEnvironmentVariableHttpRequest request
    ) {
        try {
            CreateServiceEnvironmentVariableUseCase.CreateServiceEnvironmentVariableResult result =
                    createServiceEnvironmentVariableUseCase.createServiceEnvironmentVariable(
                            new CreateServiceEnvironmentVariableUseCase.CreateServiceEnvironmentVariableCommand(
                                    new ServiceName(serviceName),
                                    new EnvironmentVariables.Key(request.key()),
                                    new EnvironmentVariables.Value(request.value())
                            )
                    );

            return switch (result) {
                case CreateServiceEnvironmentVariableUseCase.CreateServiceEnvironmentVariableResult.Success _ ->
                        ResponseEntity.status(HttpStatus.CREATED).build();
                case CreateServiceEnvironmentVariableUseCase.CreateServiceEnvironmentVariableResult.ServiceNotFound _ ->
                        ResponseEntity.notFound().build();
                case CreateServiceEnvironmentVariableUseCase.CreateServiceEnvironmentVariableResult.AlreadyExists _ ->
                        ResponseEntity.status(HttpStatus.CONFLICT).build();
                case CreateServiceEnvironmentVariableUseCase.CreateServiceEnvironmentVariableResult.Failure _ ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/environment-variables/{key}")
    public ResponseEntity<Void> updateEnvironmentVariable(
            @PathVariable String serviceName,
            @PathVariable String key,
            @RequestBody UpdateEnvironmentVariableHttpRequest request
    ) {
        try {
            UpdateServiceEnvironmentVariableUseCase.UpdateServiceEnvironmentVariableResult result =
                    updateServiceEnvironmentVariableUseCase.updateServiceEnvironmentVariable(
                            new UpdateServiceEnvironmentVariableUseCase.UpdateServiceEnvironmentVariableCommand(
                                    new ServiceName(serviceName),
                                    new EnvironmentVariables.Key(key),
                                    new EnvironmentVariables.Value(request.value())
                            )
                    );

            return switch (result) {
                case UpdateServiceEnvironmentVariableUseCase.UpdateServiceEnvironmentVariableResult.Success _ ->
                        ResponseEntity.noContent().build();
                case UpdateServiceEnvironmentVariableUseCase.UpdateServiceEnvironmentVariableResult.ServiceNotFound _ ->
                        ResponseEntity.notFound().build();
                case UpdateServiceEnvironmentVariableUseCase.UpdateServiceEnvironmentVariableResult.EnvironmentVariableNotFound _ ->
                        ResponseEntity.notFound().build();
                case UpdateServiceEnvironmentVariableUseCase.UpdateServiceEnvironmentVariableResult.Failure _ ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/environment-variables/{key}")
    public ResponseEntity<Void> deleteEnvironmentVariable(
            @PathVariable String serviceName,
            @PathVariable String key
    ) {
        try {
            DeleteServiceEnvironmentVariableUseCase.DeleteServiceEnvironmentVariableResult result =
                    deleteServiceEnvironmentVariableUseCase.deleteServiceEnvironmentVariable(
                            new DeleteServiceEnvironmentVariableUseCase.DeleteServiceEnvironmentVariableCommand(
                                    new ServiceName(serviceName),
                                    new EnvironmentVariables.Key(key)
                            )
                    );

            return switch (result) {
                case DeleteServiceEnvironmentVariableUseCase.DeleteServiceEnvironmentVariableResult.Success _ ->
                        ResponseEntity.noContent().build();
                case DeleteServiceEnvironmentVariableUseCase.DeleteServiceEnvironmentVariableResult.ServiceNotFound _ ->
                        ResponseEntity.notFound().build();
                case DeleteServiceEnvironmentVariableUseCase.DeleteServiceEnvironmentVariableResult.EnvironmentVariableNotFound _ ->
                        ResponseEntity.notFound().build();
                case DeleteServiceEnvironmentVariableUseCase.DeleteServiceEnvironmentVariableResult.Failure _ ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
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

    @PostMapping("/port-mappings")
    public ResponseEntity<Void> createPortMapping(
            @PathVariable String serviceName,
            @RequestBody CreatePortMappingHttpRequest request
    ) {
        try {
            CreateServicePortMappingUseCase.CreateServicePortMappingResult result =
                    createServicePortMappingUseCase.createServicePortMapping(
                            new CreateServicePortMappingUseCase.CreateServicePortMappingCommand(
                                    new ServiceName(serviceName),
                                    request.toHostPort(),
                                    request.toContainerPort()
                            )
                    );

            return switch (result) {
                case CreateServicePortMappingUseCase.CreateServicePortMappingResult.Success _ ->
                        ResponseEntity.status(HttpStatus.CREATED).build();
                case CreateServicePortMappingUseCase.CreateServicePortMappingResult.ServiceNotFound _ ->
                        ResponseEntity.notFound().build();
                case CreateServicePortMappingUseCase.CreateServicePortMappingResult.AlreadyExists _ ->
                        ResponseEntity.status(HttpStatus.CONFLICT).build();
                case CreateServicePortMappingUseCase.CreateServicePortMappingResult.Failure _ ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/port-mappings/{hostProtocol}/{hostPort}")
    public ResponseEntity<Void> deletePortMapping(
            @PathVariable String serviceName,
            @PathVariable String hostProtocol,
            @PathVariable int hostPort
    ) {
        try {
            DeleteServicePortMappingUseCase.DeleteServicePortMappingResult result =
                    deleteServicePortMappingUseCase.deleteServicePortMapping(
                            new DeleteServicePortMappingUseCase.DeleteServicePortMappingCommand(
                                    new ServiceName(serviceName),
                                    new Port(hostPort, Port.Protocol.valueOf(hostProtocol))
                            )
                    );

            return switch (result) {
                case DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.Success _ ->
                        ResponseEntity.noContent().build();
                case DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.ServiceNotFound _ ->
                        ResponseEntity.notFound().build();
                case DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.PortMappingNotFound _ ->
                        ResponseEntity.notFound().build();
                case DeleteServicePortMappingUseCase.DeleteServicePortMappingResult.Failure _ ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/volume-mounts")
    public ResponseEntity<?> getVolumeMounts(@PathVariable String serviceName) {
        try {
            GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult result =
                    getServiceVolumeMountsUseCase.getServiceVolumeMounts(
                            new GetServiceVolumeMountsUseCase.GetServiceVolumeMountsCommand(
                                    new ServiceName(serviceName)
                            )
                    );

            return switch (result) {
                case GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult.Success success ->
                        ResponseEntity.ok(VolumeMountHttpResponse.from(success.volumeMounts()));
                case GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult.NotFound _ ->
                        ResponseEntity.notFound().build();
                case GetServiceVolumeMountsUseCase.GetServiceVolumeMountsResult.Failure _ ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/volume-mounts")
    public ResponseEntity<Void> createVolumeMount(
            @PathVariable String serviceName,
            @RequestBody CreateVolumeMountHttpRequest request
    ) {
        try {
            CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult result =
                    createServiceVolumeMountUseCase.createServiceVolumeMount(
                            new CreateServiceVolumeMountUseCase.CreateServiceVolumeMountCommand(
                                    new ServiceName(serviceName),
                                    request.toVolumeMount()
                            )
                    );

            return switch (result) {
                case CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.Success _ ->
                        ResponseEntity.status(HttpStatus.CREATED).build();
                case CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.ServiceNotFound _ ->
                        ResponseEntity.notFound().build();
                case CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.AlreadyExists _ ->
                        ResponseEntity.status(HttpStatus.CONFLICT).build();
                case CreateServiceVolumeMountUseCase.CreateServiceVolumeMountResult.Failure _ ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/volume-mounts")
    public ResponseEntity<Void> updateVolumeMount(
            @PathVariable String serviceName,
            @RequestParam String targetPath,
            @RequestBody UpdateVolumeMountHttpRequest request
    ) {
        try {
            UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult result =
                    updateServiceVolumeMountUseCase.updateServiceVolumeMount(
                            new UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountCommand(
                                    new ServiceName(serviceName),
                                    request.toVolumeMount(new VolumeMount.Target(targetPath))
                            )
                    );

            return switch (result) {
                case UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.Success _ ->
                        ResponseEntity.noContent().build();
                case UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.ServiceNotFound _ ->
                        ResponseEntity.notFound().build();
                case UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.VolumeMountNotFound _ ->
                        ResponseEntity.notFound().build();
                case UpdateServiceVolumeMountUseCase.UpdateServiceVolumeMountResult.Failure _ ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/volume-mounts")
    public ResponseEntity<Void> deleteVolumeMount(
            @PathVariable String serviceName,
            @RequestParam String targetPath
    ) {
        try {
            DeleteServiceVolumeMountUseCase.DeleteServiceVolumeMountResult result =
                    deleteServiceVolumeMountUseCase.deleteServiceVolumeMount(
                            new DeleteServiceVolumeMountUseCase.DeleteServiceVolumeMountCommand(
                                    new ServiceName(serviceName),
                                    new VolumeMount.Target(targetPath)
                            )
                    );

            return switch (result) {
                case DeleteServiceVolumeMountUseCase.DeleteServiceVolumeMountResult.Success _ ->
                        ResponseEntity.noContent().build();
                case DeleteServiceVolumeMountUseCase.DeleteServiceVolumeMountResult.ServiceNotFound _ ->
                        ResponseEntity.notFound().build();
                case DeleteServiceVolumeMountUseCase.DeleteServiceVolumeMountResult.VolumeMountNotFound _ ->
                        ResponseEntity.notFound().build();
                case DeleteServiceVolumeMountUseCase.DeleteServiceVolumeMountResult.Failure _ ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
    }

    public record CreatePortMappingHttpRequest(
            int hostPort,
            String hostProtocol,
            int containerPort,
            String containerProtocol
    ) {

        Port toHostPort() {
            return new Port(hostPort, Port.Protocol.valueOf(hostProtocol));
        }

        Port toContainerPort() {
            return new Port(containerPort, Port.Protocol.valueOf(containerProtocol));
        }
    }

    public record CreateEnvironmentVariableHttpRequest(String key, String value) {
    }

    public record UpdateEnvironmentVariableHttpRequest(String value) {
    }

    public record CreateVolumeMountHttpRequest(
            String targetPath,
            String mountType,
            String source,
            boolean readOnly
    ) {

        VolumeMount toVolumeMount() {
            return volumeMountFrom(mountType, source, new VolumeMount.Target(targetPath), readOnly);
        }
    }

    public record UpdateVolumeMountHttpRequest(
            String mountType,
            String source,
            boolean readOnly
    ) {

        VolumeMount toVolumeMount(VolumeMount.Target target) {
            return volumeMountFrom(mountType, source, target, readOnly);
        }
    }

    private static VolumeMount volumeMountFrom(
            String mountType,
            String source,
            VolumeMount.Target target,
            boolean readOnly
    ) {
        return switch (mountType) {
            case "BIND" -> new VolumeMount.BindMount(new VolumeMount.HostPath(source), target, readOnly);
            case "VOLUME" -> new VolumeMount.NamedVolumeMount(
                    new VolumeMount.VolumeName(source),
                    target,
                    readOnly
            );
            default -> throw new IllegalArgumentException("mountType must be BIND or VOLUME");
        };
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

    public record EnvironmentVariableHttpResponse(String key, String value) {

        static List<EnvironmentVariableHttpResponse> from(EnvironmentVariables environmentVariables) {
            return environmentVariables
                    .asMap()
                    .entrySet()
                    .stream()
                    .map(entry -> new EnvironmentVariableHttpResponse(entry.getKey().value(), entry.getValue().value()))
                    .toList();
        }
    }

    public record VolumeMountHttpResponse(
            String targetPath,
            String mountType,
            String source,
            boolean readOnly
    ) {

        static List<VolumeMountHttpResponse> from(VolumeMounts volumeMounts) {
            return volumeMounts
                    .asMap()
                    .values()
                    .stream()
                    .map(VolumeMountHttpResponse::from)
                    .toList();
        }

        private static VolumeMountHttpResponse from(VolumeMount volumeMount) {
            return switch (volumeMount) {
                case VolumeMount.BindMount bindMount -> new VolumeMountHttpResponse(
                        bindMount.target().value(),
                        "BIND",
                        bindMount.source().value(),
                        bindMount.readOnly()
                );
                case VolumeMount.NamedVolumeMount namedVolumeMount -> new VolumeMountHttpResponse(
                        namedVolumeMount.target().value(),
                        "VOLUME",
                        namedVolumeMount.source().value(),
                        namedVolumeMount.readOnly()
                );
            };
        }
    }
}
