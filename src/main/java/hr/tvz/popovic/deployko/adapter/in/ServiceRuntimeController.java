package hr.tvz.popovic.deployko.adapter.in;

import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.DeployServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.StartServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.StopServiceUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/services/{serviceName}/runtime")
public class ServiceRuntimeController {

    private final DeployServiceUseCase deployServiceUseCase;
    private final StartServiceUseCase startServiceUseCase;
    private final StopServiceUseCase stopServiceUseCase;

    public ServiceRuntimeController(
            DeployServiceUseCase deployServiceUseCase,
            StartServiceUseCase startServiceUseCase,
            StopServiceUseCase stopServiceUseCase
    ) {
        this.deployServiceUseCase = deployServiceUseCase;
        this.startServiceUseCase = startServiceUseCase;
        this.stopServiceUseCase = stopServiceUseCase;
    }

    @PostMapping("/deploy")
    public ResponseEntity<Void> deployService(
            @PathVariable String serviceName,
            @RequestBody DeployServiceHttpRequest request
    ) {
        try {
            DeployServiceUseCase.DeployServiceResult result = deployServiceUseCase.deployService(
                    new DeployServiceUseCase.DeployServiceCommand(
                            new ServiceName(serviceName),
                            new ImageVersion(request.imageVersion())
                    )
            );

            return switch (result) {
                case DeployServiceUseCase.DeployServiceResult.Success _ -> ResponseEntity.noContent().build();
                case DeployServiceUseCase.DeployServiceResult.ServiceNotFound _ -> ResponseEntity.notFound().build();
                case DeployServiceUseCase.DeployServiceResult.DesiredStateFailure _,
                     DeployServiceUseCase.DeployServiceResult.DockerFailure _ -> ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/start")
    public ResponseEntity<Void> startService(@PathVariable String serviceName) {
        try {
            StartServiceUseCase.StartServiceResult result = startServiceUseCase.startService(
                    new StartServiceUseCase.StartServiceCommand(new ServiceName(serviceName))
            );

            return switch (result) {
                case StartServiceUseCase.StartServiceResult.Success _ -> ResponseEntity.noContent().build();
                case StartServiceUseCase.StartServiceResult.ServiceNotFound _,
                     StartServiceUseCase.StartServiceResult.NotDeployed _ -> ResponseEntity.notFound().build();
                case StartServiceUseCase.StartServiceResult.Drift _ -> ResponseEntity.status(HttpStatus.CONFLICT).build();
                case StartServiceUseCase.StartServiceResult.DesiredStateFailure _,
                     StartServiceUseCase.StartServiceResult.DockerFailure _ -> ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<Void> stopService(@PathVariable String serviceName) {
        try {
            StopServiceUseCase.StopServiceResult result = stopServiceUseCase.stopService(
                    new StopServiceUseCase.StopServiceCommand(new ServiceName(serviceName))
            );

            return switch (result) {
                case StopServiceUseCase.StopServiceResult.Success _ -> ResponseEntity.noContent().build();
                case StopServiceUseCase.StopServiceResult.ServiceNotFound _,
                     StopServiceUseCase.StopServiceResult.NotDeployed _ -> ResponseEntity.notFound().build();
                case StopServiceUseCase.StopServiceResult.Drift _ -> ResponseEntity.status(HttpStatus.CONFLICT).build();
                case StopServiceUseCase.StopServiceResult.DesiredStateFailure _,
                     StopServiceUseCase.StopServiceResult.DockerFailure _ -> ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
    }

    public record DeployServiceHttpRequest(String imageVersion) {
    }
}
