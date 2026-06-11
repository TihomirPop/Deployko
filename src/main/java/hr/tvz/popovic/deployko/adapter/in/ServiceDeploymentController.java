package hr.tvz.popovic.deployko.adapter.in;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentAttempt;
import hr.tvz.popovic.deployko.application.domain.model.ImageCommitSha;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.GetDeploymentHistoryUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetLatestDeploymentUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/services/{serviceName}/deployments")
public class ServiceDeploymentController {

    private final GetLatestDeploymentUseCase getLatestDeploymentUseCase;
    private final GetDeploymentHistoryUseCase getDeploymentHistoryUseCase;

    public ServiceDeploymentController(
            GetLatestDeploymentUseCase getLatestDeploymentUseCase,
            GetDeploymentHistoryUseCase getDeploymentHistoryUseCase
    ) {
        this.getLatestDeploymentUseCase = getLatestDeploymentUseCase;
        this.getDeploymentHistoryUseCase = getDeploymentHistoryUseCase;
    }

    @GetMapping
    public ResponseEntity<List<DeploymentHttpResponse>> getDeploymentHistory(
            @PathVariable String serviceName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since
    ) {
        try {
            GetDeploymentHistoryUseCase.GetDeploymentHistoryResult result =
                    getDeploymentHistoryUseCase.getDeploymentHistory(
                            new GetDeploymentHistoryUseCase.GetDeploymentHistoryCommand(
                                    new ServiceName(serviceName),
                                    Optional.ofNullable(since)
                            )
                    );

            return switch (result) {
                case GetDeploymentHistoryUseCase.GetDeploymentHistoryResult.Found found ->
                        ResponseEntity.ok(found.deploymentAttempts().stream()
                                .map(DeploymentHttpResponse::from)
                                .toList());
                case GetDeploymentHistoryUseCase.GetDeploymentHistoryResult.ServiceNotFound _ ->
                        ResponseEntity.notFound().build();
                case GetDeploymentHistoryUseCase.GetDeploymentHistoryResult.Failure _ ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            };
        } catch (IllegalArgumentException | NullPointerException _) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<DeploymentHttpResponse> getLatestDeployment(@PathVariable String serviceName) {
        try {
            GetLatestDeploymentUseCase.GetLatestDeploymentResult result =
                    getLatestDeploymentUseCase.getLatestDeployment(
                            new GetLatestDeploymentUseCase.GetLatestDeploymentCommand(new ServiceName(serviceName))
                    );

            return switch (result) {
                case GetLatestDeploymentUseCase.GetLatestDeploymentResult.Found found ->
                        ResponseEntity.ok(DeploymentHttpResponse.from(found.deploymentAttempt()));
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

    public record DeploymentHttpResponse(
            UUID deploymentId,
            String imageVersion,
            String commitSha,
            String status,
            OffsetDateTime recordedAt
    ) {

        static DeploymentHttpResponse from(DeploymentAttempt deploymentAttempt) {
            return new DeploymentHttpResponse(
                    deploymentAttempt.deploymentId().value(),
                    deploymentAttempt.imageVersion().value(),
                    commitShaValue(deploymentAttempt),
                    deploymentAttempt.status().name(),
                    deploymentAttempt.recordedAt()
            );
        }

        private static String commitShaValue(DeploymentAttempt deploymentAttempt) {
            return switch (deploymentAttempt.commitSha()) {
                case ImageCommitSha.Known known -> known.value();
                case ImageCommitSha.Unknown _ -> null;
            };
        }
    }
}
