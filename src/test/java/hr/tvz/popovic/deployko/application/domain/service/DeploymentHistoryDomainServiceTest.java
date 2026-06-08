package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentAttempt;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentStatus;
import hr.tvz.popovic.deployko.application.domain.model.ImageCommitSha;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.GetLatestDeploymentUseCase;
import hr.tvz.popovic.deployko.application.port.out.FindLatestDeploymentPort;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
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

    @Test
    void returns_latest_deployment_when_port_finds_attempt() {
        DeploymentHistoryDomainService service = new DeploymentHistoryDomainService(
                _ -> new FindLatestDeploymentPort.FindLatestDeploymentResult.Found(DEPLOYMENT_ATTEMPT)
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
                _ -> new FindLatestDeploymentPort.FindLatestDeploymentResult.NotDeployed()
        );

        GetLatestDeploymentUseCase.GetLatestDeploymentResult result = service.getLatestDeployment(
                new GetLatestDeploymentUseCase.GetLatestDeploymentCommand(SERVICE_NAME)
        );

        assertThat(result).isInstanceOf(GetLatestDeploymentUseCase.GetLatestDeploymentResult.NotDeployed.class);
    }

    @Test
    void returns_service_not_found_when_port_reports_missing_service() {
        DeploymentHistoryDomainService service = new DeploymentHistoryDomainService(
                _ -> new FindLatestDeploymentPort.FindLatestDeploymentResult.ServiceNotFound()
        );

        GetLatestDeploymentUseCase.GetLatestDeploymentResult result = service.getLatestDeployment(
                new GetLatestDeploymentUseCase.GetLatestDeploymentCommand(SERVICE_NAME)
        );

        assertThat(result).isInstanceOf(GetLatestDeploymentUseCase.GetLatestDeploymentResult.ServiceNotFound.class);
    }

    @Test
    void returns_failure_when_port_fails() {
        DeploymentHistoryDomainService service = new DeploymentHistoryDomainService(
                _ -> new FindLatestDeploymentPort.FindLatestDeploymentResult.Failure()
        );

        GetLatestDeploymentUseCase.GetLatestDeploymentResult result = service.getLatestDeployment(
                new GetLatestDeploymentUseCase.GetLatestDeploymentCommand(SERVICE_NAME)
        );

        assertThat(result).isInstanceOf(GetLatestDeploymentUseCase.GetLatestDeploymentResult.Failure.class);
    }
}
