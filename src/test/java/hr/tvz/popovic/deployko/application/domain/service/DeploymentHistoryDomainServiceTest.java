package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentAttempt;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentStatus;
import hr.tvz.popovic.deployko.application.domain.model.ImageCommitSha;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.GetDeploymentHistoryUseCase;
import hr.tvz.popovic.deployko.application.port.in.GetLatestDeploymentUseCase;
import hr.tvz.popovic.deployko.application.port.out.FindDeploymentHistoryPort;
import hr.tvz.popovic.deployko.application.port.out.FindLatestDeploymentPort;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentHistoryDomainServiceTest {

    private static final ServiceName SERVICE_NAME = new ServiceName("billing-api");
    private static final DeploymentAttempt DEPLOYMENT_ATTEMPT = new DeploymentAttempt(
            new DeploymentId(UUID.fromString("018f4b5d-9c64-7000-9f2e-4d8fbf9f1b22")),
            new ImageVersion("1.0.0"),
            new ImageCommitSha.Known("f5a1c2d"),
            DeploymentStatus.SUCCESS,
            OffsetDateTime.parse("2026-06-07T10:15:30Z")
    );

    private static final FindDeploymentHistoryPort UNUSED_HISTORY_PORT =
            (_, _) -> new FindDeploymentHistoryPort.FindDeploymentHistoryResult.Failure();
    private static final FindLatestDeploymentPort UNUSED_LATEST_PORT =
            _ -> new FindLatestDeploymentPort.FindLatestDeploymentResult.Failure();

    @Test
    void returns_latest_deployment_when_port_finds_attempt() {
        DeploymentHistoryDomainService service = new DeploymentHistoryDomainService(
                _ -> new FindLatestDeploymentPort.FindLatestDeploymentResult.Found(DEPLOYMENT_ATTEMPT),
                UNUSED_HISTORY_PORT
        );

        GetLatestDeploymentUseCase.GetLatestDeploymentResult result = service.getLatestDeployment(
                new GetLatestDeploymentUseCase.GetLatestDeploymentCommand(SERVICE_NAME)
        );

        assertThat(result).isEqualTo(new GetLatestDeploymentUseCase.GetLatestDeploymentResult.Found(
                DEPLOYMENT_ATTEMPT
        ));
    }

    @Test
    void returns_not_deployed_when_port_finds_no_history() {
        DeploymentHistoryDomainService service = new DeploymentHistoryDomainService(
                _ -> new FindLatestDeploymentPort.FindLatestDeploymentResult.NotDeployed(),
                UNUSED_HISTORY_PORT
        );

        GetLatestDeploymentUseCase.GetLatestDeploymentResult result = service.getLatestDeployment(
                new GetLatestDeploymentUseCase.GetLatestDeploymentCommand(SERVICE_NAME)
        );

        assertThat(result).isInstanceOf(GetLatestDeploymentUseCase.GetLatestDeploymentResult.NotDeployed.class);
    }

    @Test
    void returns_service_not_found_when_port_reports_missing_service() {
        DeploymentHistoryDomainService service = new DeploymentHistoryDomainService(
                _ -> new FindLatestDeploymentPort.FindLatestDeploymentResult.ServiceNotFound(),
                UNUSED_HISTORY_PORT
        );

        GetLatestDeploymentUseCase.GetLatestDeploymentResult result = service.getLatestDeployment(
                new GetLatestDeploymentUseCase.GetLatestDeploymentCommand(SERVICE_NAME)
        );

        assertThat(result).isInstanceOf(GetLatestDeploymentUseCase.GetLatestDeploymentResult.ServiceNotFound.class);
    }

    @Test
    void returns_failure_when_port_fails() {
        DeploymentHistoryDomainService service = new DeploymentHistoryDomainService(
                _ -> new FindLatestDeploymentPort.FindLatestDeploymentResult.Failure(),
                UNUSED_HISTORY_PORT
        );

        GetLatestDeploymentUseCase.GetLatestDeploymentResult result = service.getLatestDeployment(
                new GetLatestDeploymentUseCase.GetLatestDeploymentCommand(SERVICE_NAME)
        );

        assertThat(result).isInstanceOf(GetLatestDeploymentUseCase.GetLatestDeploymentResult.Failure.class);
    }

    @Test
    void returns_deployment_history_when_port_finds_attempts() {
        DeploymentHistoryDomainService service = new DeploymentHistoryDomainService(
                UNUSED_LATEST_PORT,
                (_, _) -> new FindDeploymentHistoryPort.FindDeploymentHistoryResult.Found(List.of(DEPLOYMENT_ATTEMPT))
        );

        GetDeploymentHistoryUseCase.GetDeploymentHistoryResult result = service.getDeploymentHistory(
                new GetDeploymentHistoryUseCase.GetDeploymentHistoryCommand(SERVICE_NAME, Optional.empty())
        );

        assertThat(result).isEqualTo(new GetDeploymentHistoryUseCase.GetDeploymentHistoryResult.Found(
                List.of(DEPLOYMENT_ATTEMPT)
        ));
    }

    @Test
    void passes_service_name_and_since_to_history_port() {
        OffsetDateTime since = OffsetDateTime.parse("2026-06-07T10:15:30Z");
        ServiceName[] capturedServiceName = new ServiceName[1];
        @SuppressWarnings("unchecked")
        Optional<OffsetDateTime>[] capturedSince = new Optional[1];
        DeploymentHistoryDomainService service = new DeploymentHistoryDomainService(
                UNUSED_LATEST_PORT,
                (serviceName, sinceArgument) -> {
                    capturedServiceName[0] = serviceName;
                    capturedSince[0] = sinceArgument;
                    return new FindDeploymentHistoryPort.FindDeploymentHistoryResult.Found(List.of());
                }
        );

        service.getDeploymentHistory(
                new GetDeploymentHistoryUseCase.GetDeploymentHistoryCommand(SERVICE_NAME, Optional.of(since))
        );

        assertThat(capturedServiceName[0]).isEqualTo(SERVICE_NAME);
        assertThat(capturedSince[0]).contains(since);
    }

    @Test
    void returns_history_service_not_found_when_port_reports_missing_service() {
        DeploymentHistoryDomainService service = new DeploymentHistoryDomainService(
                UNUSED_LATEST_PORT,
                (_, _) -> new FindDeploymentHistoryPort.FindDeploymentHistoryResult.ServiceNotFound()
        );

        GetDeploymentHistoryUseCase.GetDeploymentHistoryResult result = service.getDeploymentHistory(
                new GetDeploymentHistoryUseCase.GetDeploymentHistoryCommand(SERVICE_NAME, Optional.empty())
        );

        assertThat(result).isInstanceOf(GetDeploymentHistoryUseCase.GetDeploymentHistoryResult.ServiceNotFound.class);
    }

    @Test
    void returns_history_failure_when_port_fails() {
        DeploymentHistoryDomainService service = new DeploymentHistoryDomainService(
                UNUSED_LATEST_PORT,
                (_, _) -> new FindDeploymentHistoryPort.FindDeploymentHistoryResult.Failure()
        );

        GetDeploymentHistoryUseCase.GetDeploymentHistoryResult result = service.getDeploymentHistory(
                new GetDeploymentHistoryUseCase.GetDeploymentHistoryCommand(SERVICE_NAME, Optional.empty())
        );

        assertThat(result).isInstanceOf(GetDeploymentHistoryUseCase.GetDeploymentHistoryResult.Failure.class);
    }
}
