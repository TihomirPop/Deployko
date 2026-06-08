package hr.tvz.popovic.deployko.adapter.in;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentAttempt;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.GetLatestDeploymentUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/services/{serviceName}/deployments")
public class ServiceDeploymentController {

    private final GetLatestDeploymentUseCase getLatestDeploymentUseCase;

    public ServiceDeploymentController(GetLatestDeploymentUseCase getLatestDeploymentUseCase) {
        this.getLatestDeploymentUseCase = getLatestDeploymentUseCase;
    }

    @GetMapping("/latest")
    public ResponseEntity<LatestDeploymentHttpResponse> getLatestDeployment(@PathVariable String serviceName) {
        try {
            GetLatestDeploymentUseCase.GetLatestDeploymentResult result =
                    getLatestDeploymentUseCase.getLatestDeployment(
                            new GetLatestDeploymentUseCase.GetLatestDeploymentCommand(new ServiceName(serviceName))
                    );

            return switch (result) {
                case GetLatestDeploymentUseCase.GetLatestDeploymentResult.Found found ->
                        ResponseEntity.ok(LatestDeploymentHttpResponse.from(found.deploymentAttempt()));
                case GetLatestDeploymentUseCase.GetLatestDeploymentResult.NotDeployed _ ->
                        ResponseEntity.noContent().build();
                case GetLatestDeploymentUseCase.GetLatestDeploymentResult.ServiceNotFound _ ->
                        ResponseEntity.notFound().build();
                case GetLatestDeploymentUseCase.GetLatestDeploymentResult.Failure _ ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
    }

    public record LatestDeploymentHttpResponse(
            UUID deploymentId,
            String imageVersion,
            String commitSha,
            String status,
            OffsetDateTime recordedAt
    ) {

        static LatestDeploymentHttpResponse from(DeploymentAttempt deploymentAttempt) {
            return new LatestDeploymentHttpResponse(
                    deploymentAttempt.deploymentId().value(),
                    deploymentAttempt.imageVersion().value(),
                    commitShaValue(deploymentAttempt),
                    deploymentAttempt.status().name(),
                    deploymentAttempt.recordedAt()
            );
        }

        private static String commitShaValue(DeploymentAttempt deploymentAttempt) {
            return switch (deploymentAttempt.commitSha()) {
                case hr.tvz.popovic.deployko.application.domain.model.ImageCommitSha.Known known -> known.value();
                case hr.tvz.popovic.deployko.application.domain.model.ImageCommitSha.Unknown _ -> null;
            };
        }
    }
}
