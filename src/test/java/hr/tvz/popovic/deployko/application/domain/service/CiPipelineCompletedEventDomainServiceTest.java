package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.DeployServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.HandleCiPipelineCompletedEventUseCase;
import hr.tvz.popovic.deployko.application.port.out.FindLastCiDeploymentPort;
import hr.tvz.popovic.deployko.application.port.out.RecordCiDeploymentPort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CiPipelineCompletedEventDomainServiceTest {

    private static final ServiceName SERVICE_NAME = new ServiceName("deployko");
    private static final ImageVersion IMAGE_VERSION = new ImageVersion("43-360b816");
    private static final Instant NOW = Instant.parse("2026-06-05T12:00:00Z");
    private static final OffsetDateTime NOW_OFFSET = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration THROTTLE_WINDOW = Duration.ofMinutes(5);

    @Test
    void deploys_and_records_when_service_has_no_previous_ci_deployment() {
        FakeDeployServiceUseCase deployServiceUseCase = new FakeDeployServiceUseCase(
                new DeployServiceUseCase.DeployServiceResult.Success()
        );
        FakeRecordCiDeploymentPort recordCiDeploymentPort = new FakeRecordCiDeploymentPort(
                new RecordCiDeploymentPort.RecordCiDeploymentResult.Recorded()
        );
        CiPipelineCompletedEventDomainService service = service(
                _ -> new FindLastCiDeploymentPort.FindLastCiDeploymentResult.NotDeployed(),
                recordCiDeploymentPort,
                deployServiceUseCase
        );

        HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult result =
                service.handleCiPipelineCompletedEvent(command());

        assertThat(result)
                .isInstanceOf(HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.Deployed.class);
        assertThat(deployServiceUseCase.commands).containsExactly(new DeployServiceUseCase.DeployServiceCommand(
                SERVICE_NAME,
                IMAGE_VERSION
        ));
        assertThat(recordCiDeploymentPort.records).containsExactly(new CiDeploymentRecord(SERVICE_NAME, NOW_OFFSET));
    }

    @Test
    void deploys_when_previous_ci_deployment_is_older_than_throttle_window() {
        FakeDeployServiceUseCase deployServiceUseCase = new FakeDeployServiceUseCase(
                new DeployServiceUseCase.DeployServiceResult.Success()
        );
        CiPipelineCompletedEventDomainService service = service(
                _ -> new FindLastCiDeploymentPort.FindLastCiDeploymentResult.Found(NOW_OFFSET.minusMinutes(6)),
                new FakeRecordCiDeploymentPort(new RecordCiDeploymentPort.RecordCiDeploymentResult.Recorded()),
                deployServiceUseCase
        );

        HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult result =
                service.handleCiPipelineCompletedEvent(command());

        assertThat(result)
                .isInstanceOf(HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.Deployed.class);
        assertThat(deployServiceUseCase.commands).hasSize(1);
    }

    @Test
    void skips_when_previous_ci_deployment_is_inside_throttle_window() {
        FakeDeployServiceUseCase deployServiceUseCase = new FakeDeployServiceUseCase(
                new DeployServiceUseCase.DeployServiceResult.Success()
        );
        CiPipelineCompletedEventDomainService service = service(
                _ -> new FindLastCiDeploymentPort.FindLastCiDeploymentResult.Found(NOW_OFFSET.minusMinutes(4)),
                new FakeRecordCiDeploymentPort(new RecordCiDeploymentPort.RecordCiDeploymentResult.Recorded()),
                deployServiceUseCase
        );

        HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult result =
                service.handleCiPipelineCompletedEvent(command());

        assertThat(result)
                .isInstanceOf(HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.SkippedRecentDeployment.class);
        assertThat(deployServiceUseCase.commands).isEmpty();
    }

    @Test
    void returns_service_not_found_when_last_deployment_lookup_reports_missing_service() {
        CiPipelineCompletedEventDomainService service = service(
                _ -> new FindLastCiDeploymentPort.FindLastCiDeploymentResult.ServiceNotFound(),
                new FakeRecordCiDeploymentPort(new RecordCiDeploymentPort.RecordCiDeploymentResult.Recorded()),
                new FakeDeployServiceUseCase(new DeployServiceUseCase.DeployServiceResult.Success())
        );

        HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult result =
                service.handleCiPipelineCompletedEvent(command());

        assertThat(result)
                .isInstanceOf(HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.ServiceNotFound.class);
    }

    @Test
    void returns_lookup_failure_when_last_deployment_lookup_fails() {
        CiPipelineCompletedEventDomainService service = service(
                _ -> new FindLastCiDeploymentPort.FindLastCiDeploymentResult.Failure(),
                new FakeRecordCiDeploymentPort(new RecordCiDeploymentPort.RecordCiDeploymentResult.Recorded()),
                new FakeDeployServiceUseCase(new DeployServiceUseCase.DeployServiceResult.Success())
        );

        HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult result =
                service.handleCiPipelineCompletedEvent(command());

        assertThat(result)
                .isInstanceOf(HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.LastDeploymentLookupFailure.class);
    }

    @Test
    void returns_deployment_failure_when_deploy_use_case_fails() {
        FakeRecordCiDeploymentPort recordCiDeploymentPort = new FakeRecordCiDeploymentPort(
                new RecordCiDeploymentPort.RecordCiDeploymentResult.Recorded()
        );
        CiPipelineCompletedEventDomainService service = service(
                _ -> new FindLastCiDeploymentPort.FindLastCiDeploymentResult.NotDeployed(),
                recordCiDeploymentPort,
                new FakeDeployServiceUseCase(new DeployServiceUseCase.DeployServiceResult.DockerFailure())
        );

        HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult result =
                service.handleCiPipelineCompletedEvent(command());

        assertThat(result)
                .isInstanceOf(HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.DeploymentFailure.class);
        assertThat(recordCiDeploymentPort.records).isEmpty();
    }

    @Test
    void returns_record_failure_when_successful_deployment_cannot_be_recorded() {
        CiPipelineCompletedEventDomainService service = service(
                _ -> new FindLastCiDeploymentPort.FindLastCiDeploymentResult.NotDeployed(),
                new FakeRecordCiDeploymentPort(new RecordCiDeploymentPort.RecordCiDeploymentResult.Failure()),
                new FakeDeployServiceUseCase(new DeployServiceUseCase.DeployServiceResult.Success())
        );

        HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult result =
                service.handleCiPipelineCompletedEvent(command());

        assertThat(result)
                .isInstanceOf(HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.RecordDeploymentFailure.class);
    }

    private static CiPipelineCompletedEventDomainService service(
            FindLastCiDeploymentPort findLastCiDeploymentPort,
            FakeRecordCiDeploymentPort recordCiDeploymentPort,
            FakeDeployServiceUseCase deployServiceUseCase
    ) {
        return new CiPipelineCompletedEventDomainService(
                findLastCiDeploymentPort,
                recordCiDeploymentPort,
                deployServiceUseCase,
                CLOCK,
                THROTTLE_WINDOW
        );
    }

    private static HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventCommand command() {
        return new HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventCommand(
                SERVICE_NAME,
                IMAGE_VERSION,
                43
        );
    }

    private record CiDeploymentRecord(ServiceName serviceName, OffsetDateTime deployedAt) {
    }

    private static final class FakeDeployServiceUseCase implements DeployServiceUseCase {

        private final DeployServiceResult result;
        private final List<DeployServiceCommand> commands = new ArrayList<>();

        private FakeDeployServiceUseCase(DeployServiceResult result) {
            this.result = result;
        }

        @Override
        public DeployServiceResult deployService(DeployServiceCommand command) {
            commands.add(command);
            return result;
        }
    }

    private static final class FakeRecordCiDeploymentPort implements RecordCiDeploymentPort {

        private final RecordCiDeploymentResult result;
        private final List<CiDeploymentRecord> records = new ArrayList<>();

        private FakeRecordCiDeploymentPort(RecordCiDeploymentResult result) {
            this.result = result;
        }

        @Override
        public RecordCiDeploymentResult recordCiDeployment(ServiceName serviceName, OffsetDateTime deployedAt) {
            records.add(new CiDeploymentRecord(serviceName, deployedAt));
            return result;
        }
    }
}
