package hr.tvz.popovic.deployko.adapter.in;

import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.ServiceDeploymentUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/services/{serviceName}/deployments")
public class ServiceDeploymentController {

    private final ServiceDeploymentUseCase serviceDeploymentUseCase;

    public ServiceDeploymentController(ServiceDeploymentUseCase serviceDeploymentUseCase) {
        this.serviceDeploymentUseCase = serviceDeploymentUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> deployService(
            @PathVariable String serviceName,
            @RequestBody DeployServiceHttpRequest request
    ) {
        try {
            ServiceDeploymentUseCase.DeployServiceResult result = serviceDeploymentUseCase.deployService(
                    new ServiceDeploymentUseCase.DeployServiceCommand(
                            new ServiceName(serviceName),
                            new ImageVersion(request.imageVersion())
                    )
            );

            return switch (result) {
                case ServiceDeploymentUseCase.DeployServiceResult.Success _ -> ResponseEntity.noContent().build();
                case ServiceDeploymentUseCase.DeployServiceResult.ServiceNotFound _ -> ResponseEntity.notFound().build();
                case ServiceDeploymentUseCase.DeployServiceResult.DesiredStateFailure _,
                     ServiceDeploymentUseCase.DeployServiceResult.DockerFailure _ -> ResponseEntity
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
            ServiceDeploymentUseCase.StartServiceResult result = serviceDeploymentUseCase.startService(
                    new ServiceDeploymentUseCase.StartServiceCommand(new ServiceName(serviceName))
            );

            return switch (result) {
                case ServiceDeploymentUseCase.StartServiceResult.Success _ -> ResponseEntity.noContent().build();
                case ServiceDeploymentUseCase.StartServiceResult.ServiceNotFound _,
                     ServiceDeploymentUseCase.StartServiceResult.NotDeployed _ -> ResponseEntity.notFound().build();
                case ServiceDeploymentUseCase.StartServiceResult.Drift _ -> ResponseEntity.status(HttpStatus.CONFLICT).build();
                case ServiceDeploymentUseCase.StartServiceResult.DesiredStateFailure _,
                     ServiceDeploymentUseCase.StartServiceResult.DockerFailure _ -> ResponseEntity
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
            ServiceDeploymentUseCase.StopServiceResult result = serviceDeploymentUseCase.stopService(
                    new ServiceDeploymentUseCase.StopServiceCommand(new ServiceName(serviceName))
            );

            return switch (result) {
                case ServiceDeploymentUseCase.StopServiceResult.Success _ -> ResponseEntity.noContent().build();
                case ServiceDeploymentUseCase.StopServiceResult.ServiceNotFound _,
                     ServiceDeploymentUseCase.StopServiceResult.NotDeployed _ -> ResponseEntity.notFound().build();
                case ServiceDeploymentUseCase.StopServiceResult.Drift _ -> ResponseEntity.status(HttpStatus.CONFLICT).build();
                case ServiceDeploymentUseCase.StopServiceResult.DesiredStateFailure _,
                     ServiceDeploymentUseCase.StopServiceResult.DockerFailure _ -> ResponseEntity
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
