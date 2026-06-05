package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.DeployServiceUseCase;
import hr.tvz.popovic.deployko.application.port.in.HandleCiPipelineCompletedEventUseCase;
import hr.tvz.popovic.deployko.application.port.out.FindLastCiDeploymentPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceNamesByImageRepositoryPort;
import hr.tvz.popovic.deployko.application.port.out.RecordCiDeploymentPort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class CiPipelineCompletedEventDomainServiceTest {

    private static final ImageRepository IMAGE_REPOSITORY = new ImageRepository("ghcr.io/deployko/api");
    private static final ImageVersion IMAGE_VERSION = new ImageVersion("43-360b816");
    private static final ServiceName API_SERVICE_NAME = new ServiceName("deployko-api");
    private static final ServiceName WORKER_SERVICE_NAME = new ServiceName("deployko-worker");
    private static final Instant NOW = Instant.parse("2026-06-05T12:00:00Z");
    private static final OffsetDateTime NOW_OFFSET = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration THROTTLE_WINDOW = Duration.ofMinutes(5);

    @Test
    void deploys_and_records_all_services_matching_image_repository() {
        FakeDeployServiceUseCase deployServiceUseCase = new FakeDeployServiceUseCase(
                new DeployServiceUseCase.DeployServiceResult.Success()
        );
        FakeRecordCiDeploymentPort recordCiDeploymentPort = new FakeRecordCiDeploymentPort(
                new RecordCiDeploymentPort.RecordCiDeploymentResult.Recorded()
        );
        CiPipelineCompletedEventDomainService service = service(
                services(API_SERVICE_NAME, WORKER_SERVICE_NAME),
                _ -> new FindLastCiDeploymentPort.FindLastCiDeploymentResult.NotDeployed(),
                recordCiDeploymentPort,
                deployServiceUseCase
        );

        HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult result =
                service.handleCiPipelineCompletedEvent(command());

        assertThat(result)
                .isInstanceOf(HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.Deployed.class);
        assertThat(deployServiceUseCase.commands).containsExactly(
                new DeployServiceUseCase.DeployServiceCommand(API_SERVICE_NAME, IMAGE_VERSION),
                new DeployServiceUseCase.DeployServiceCommand(WORKER_SERVICE_NAME, IMAGE_VERSION)
        );
        assertThat(recordCiDeploymentPort.records).containsExactly(
                new CiDeploymentRecord(API_SERVICE_NAME, NOW_OFFSET),
                new CiDeploymentRecord(WORKER_SERVICE_NAME, NOW_OFFSET)
        );
    }

    @Test
    void deploys_only_services_outside_throttle_window() {
        FakeDeployServiceUseCase deployServiceUseCase = new FakeDeployServiceUseCase(
                new DeployServiceUseCase.DeployServiceResult.Success()
        );
        FakeRecordCiDeploymentPort recordCiDeploymentPort = new FakeRecordCiDeploymentPort(
                new RecordCiDeploymentPort.RecordCiDeploymentResult.Recorded()
        );
        CiPipelineCompletedEventDomainService service = service(
                services(API_SERVICE_NAME, WORKER_SERVICE_NAME),
                serviceName -> {
                    if (serviceName.equals(API_SERVICE_NAME)) {
                        return new FindLastCiDeploymentPort.FindLastCiDeploymentResult.Found(NOW_OFFSET.minusMinutes(4));
                    }
                    return new FindLastCiDeploymentPort.FindLastCiDeploymentResult.Found(NOW_OFFSET.minusMinutes(6));
                },
                recordCiDeploymentPort,
                deployServiceUseCase
        );

        HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult result =
                service.handleCiPipelineCompletedEvent(command());

        assertThat(result)
                .isInstanceOf(HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.Deployed.class);
        assertThat(deployServiceUseCase.commands).containsExactly(
                new DeployServiceUseCase.DeployServiceCommand(WORKER_SERVICE_NAME, IMAGE_VERSION)
        );
        assertThat(recordCiDeploymentPort.records).containsExactly(
                new CiDeploymentRecord(WORKER_SERVICE_NAME, NOW_OFFSET)
        );
    }

    @Test
    void skips_when_all_matching_services_are_inside_throttle_window() {
        FakeDeployServiceUseCase deployServiceUseCase = new FakeDeployServiceUseCase(
                new DeployServiceUseCase.DeployServiceResult.Success()
        );
        CiPipelineCompletedEventDomainService service = service(
                services(API_SERVICE_NAME, WORKER_SERVICE_NAME),
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
    void returns_no_matching_services_when_repository_has_no_services() {
        CiPipelineCompletedEventDomainService service = service(
                services(),
                _ -> new FindLastCiDeploymentPort.FindLastCiDeploymentResult.NotDeployed(),
                new FakeRecordCiDeploymentPort(new RecordCiDeploymentPort.RecordCiDeploymentResult.Recorded()),
                new FakeDeployServiceUseCase(new DeployServiceUseCase.DeployServiceResult.Success())
        );

        HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult result =
                service.handleCiPipelineCompletedEvent(command());

        assertThat(result)
                .isInstanceOf(HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.NoMatchingServices.class);
    }

    @Test
    void returns_service_lookup_failure_when_repository_lookup_fails() {
        CiPipelineCompletedEventDomainService service = service(
                _ -> new FindServiceNamesByImageRepositoryPort.FindServiceNamesByImageRepositoryResult.Failure(),
                _ -> new FindLastCiDeploymentPort.FindLastCiDeploymentResult.NotDeployed(),
                new FakeRecordCiDeploymentPort(new RecordCiDeploymentPort.RecordCiDeploymentResult.Recorded()),
                new FakeDeployServiceUseCase(new DeployServiceUseCase.DeployServiceResult.Success())
        );

        HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult result =
                service.handleCiPipelineCompletedEvent(command());

        assertThat(result)
                .isInstanceOf(HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.ServiceLookupFailure.class);
    }

    @Test
    void returns_lookup_failure_when_last_deployment_lookup_fails_for_any_matching_service() {
        CiPipelineCompletedEventDomainService service = service(
                services(API_SERVICE_NAME),
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
    void returns_deployment_failure_when_deploy_use_case_fails_for_any_matching_service() {
        FakeRecordCiDeploymentPort recordCiDeploymentPort = new FakeRecordCiDeploymentPort(
                new RecordCiDeploymentPort.RecordCiDeploymentResult.Recorded()
        );
        CiPipelineCompletedEventDomainService service = service(
                services(API_SERVICE_NAME),
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
    void attempts_remaining_services_when_one_matching_service_fails() {
        FakeRecordCiDeploymentPort recordCiDeploymentPort = new FakeRecordCiDeploymentPort(
                new RecordCiDeploymentPort.RecordCiDeploymentResult.Recorded()
        );
        FakeDeployServiceUseCase deployServiceUseCase = new FakeDeployServiceUseCase(command -> {
            if (command.serviceName().equals(API_SERVICE_NAME)) {
                return new DeployServiceUseCase.DeployServiceResult.DockerFailure();
            }
            return new DeployServiceUseCase.DeployServiceResult.Success();
        });
        CiPipelineCompletedEventDomainService service = service(
                services(API_SERVICE_NAME, WORKER_SERVICE_NAME),
                _ -> new FindLastCiDeploymentPort.FindLastCiDeploymentResult.NotDeployed(),
                recordCiDeploymentPort,
                deployServiceUseCase
        );

        HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult result =
                service.handleCiPipelineCompletedEvent(command());

        assertThat(result)
                .isInstanceOf(HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.DeploymentFailure.class);
        assertThat(deployServiceUseCase.commands).containsExactly(
                new DeployServiceUseCase.DeployServiceCommand(API_SERVICE_NAME, IMAGE_VERSION),
                new DeployServiceUseCase.DeployServiceCommand(WORKER_SERVICE_NAME, IMAGE_VERSION)
        );
        assertThat(recordCiDeploymentPort.records).containsExactly(new CiDeploymentRecord(WORKER_SERVICE_NAME, NOW_OFFSET));
    }

    @Test
    void returns_record_failure_when_successful_deployment_cannot_be_recorded() {
        CiPipelineCompletedEventDomainService service = service(
                services(API_SERVICE_NAME),
                _ -> new FindLastCiDeploymentPort.FindLastCiDeploymentResult.NotDeployed(),
                new FakeRecordCiDeploymentPort(new RecordCiDeploymentPort.RecordCiDeploymentResult.Failure()),
                new FakeDeployServiceUseCase(new DeployServiceUseCase.DeployServiceResult.Success())
        );

        HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult result =
                service.handleCiPipelineCompletedEvent(command());

        assertThat(result)
                .isInstanceOf(HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.RecordDeploymentFailure.class);
    }

    private static FindServiceNamesByImageRepositoryPort services(ServiceName... serviceNames) {
        return _ -> new FindServiceNamesByImageRepositoryPort.FindServiceNamesByImageRepositoryResult.Found(
                List.of(serviceNames)
        );
    }

    private static CiPipelineCompletedEventDomainService service(
            FindServiceNamesByImageRepositoryPort findServiceNamesByImageRepositoryPort,
            FindLastCiDeploymentPort findLastCiDeploymentPort,
            FakeRecordCiDeploymentPort recordCiDeploymentPort,
            FakeDeployServiceUseCase deployServiceUseCase
    ) {
        return new CiPipelineCompletedEventDomainService(
                findServiceNamesByImageRepositoryPort,
                findLastCiDeploymentPort,
                recordCiDeploymentPort,
                deployServiceUseCase,
                CLOCK,
                THROTTLE_WINDOW
        );
    }

    private static HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventCommand command() {
        return new HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventCommand(
                IMAGE_REPOSITORY,
                IMAGE_VERSION,
                43
        );
    }

    private record CiDeploymentRecord(ServiceName serviceName, OffsetDateTime deployedAt) {
    }

    private static final class FakeDeployServiceUseCase implements DeployServiceUseCase {

        private final Function<DeployServiceCommand, DeployServiceResult> results;
        private final List<DeployServiceCommand> commands = new ArrayList<>();

        private FakeDeployServiceUseCase(DeployServiceResult result) {
            this(_ -> result);
        }

        private FakeDeployServiceUseCase(Function<DeployServiceCommand, DeployServiceResult> results) {
            this.results = results;
        }

        @Override
        public DeployServiceResult deployService(DeployServiceCommand command) {
            commands.add(command);
            return results.apply(command);
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
