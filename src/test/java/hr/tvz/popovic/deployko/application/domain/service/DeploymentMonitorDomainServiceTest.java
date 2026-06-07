package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.ActualDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentStatus;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.RuntimeConfiguration;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.out.FindActualDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.FindDesiredDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.UpdateDeploymentStatusPort;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentMonitorDomainServiceTest {

    private static final ServiceName SERVICE_NAME = new ServiceName("billing-api");
    private static final DeploymentId DEPLOYMENT_ID = new DeploymentId(
            UUID.fromString("018f4b5d-9c64-7000-9f2e-4d8fbf9f1b22")
    );

    @Test
    void marks_deployment_success_after_required_consecutive_running_checks() throws InterruptedException {
        RecordingUpdateDeploymentStatusPort updateDeploymentStatusPort = new RecordingUpdateDeploymentStatusPort();
        try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            DeploymentMonitorDomainService monitor = new DeploymentMonitorDomainService(
                    _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found(
                            ActualDeploymentState.RUNNING
                    ),
                    currentDesiredStatePort(DesiredDeploymentState.RUNNING),
                    updateDeploymentStatusPort,
                    executorService,
                    Duration.ofSeconds(1),
                    Duration.ZERO,
                    2,
                    false
            );

            monitor.monitorDeployment(desiredDeployment(DesiredDeploymentState.RUNNING), DEPLOYMENT_ID);

            assertThat(updateDeploymentStatusPort.awaitStatus()).isTrue();
            assertThat(updateDeploymentStatusPort.records)
                    .containsExactly(new DeploymentStatusRecord(DEPLOYMENT_ID, DeploymentStatus.SUCCESS));
        }
    }

    @Test
    void marks_stopped_deployment_success_when_container_is_not_running() throws InterruptedException {
        RecordingUpdateDeploymentStatusPort updateDeploymentStatusPort = new RecordingUpdateDeploymentStatusPort();
        try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            DeploymentMonitorDomainService monitor = new DeploymentMonitorDomainService(
                    _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found(
                            ActualDeploymentState.MISSING
                    ),
                    currentDesiredStatePort(DesiredDeploymentState.STOPPED),
                    updateDeploymentStatusPort,
                    executorService,
                    Duration.ofSeconds(1),
                    Duration.ZERO,
                    1,
                    false
            );

            monitor.monitorDeployment(desiredDeployment(DesiredDeploymentState.STOPPED), DEPLOYMENT_ID);

            assertThat(updateDeploymentStatusPort.awaitStatus()).isTrue();
            assertThat(updateDeploymentStatusPort.records)
                    .containsExactly(new DeploymentStatusRecord(DEPLOYMENT_ID, DeploymentStatus.SUCCESS));
        }
    }

    @Test
    void resets_running_counter_when_container_stops_between_checks() throws InterruptedException {
        RecordingUpdateDeploymentStatusPort updateDeploymentStatusPort = new RecordingUpdateDeploymentStatusPort();
        List<ActualDeploymentState> states = new ArrayList<>(List.of(
                ActualDeploymentState.RUNNING,
                ActualDeploymentState.STOPPED,
                ActualDeploymentState.RUNNING,
                ActualDeploymentState.RUNNING
        ));
        try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            DeploymentMonitorDomainService monitor = new DeploymentMonitorDomainService(
                    _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found(states.removeFirst()),
                    currentDesiredStatePort(DesiredDeploymentState.RUNNING),
                    updateDeploymentStatusPort,
                    executorService,
                    Duration.ofSeconds(1),
                    Duration.ZERO,
                    2,
                    false
            );

            monitor.monitorDeployment(desiredDeployment(DesiredDeploymentState.RUNNING), DEPLOYMENT_ID);

            assertThat(updateDeploymentStatusPort.awaitStatus()).isTrue();
            assertThat(states).isEmpty();
            assertThat(updateDeploymentStatusPort.records)
                    .containsExactly(new DeploymentStatusRecord(DEPLOYMENT_ID, DeploymentStatus.SUCCESS));
        }
    }

    @Test
    void marks_deployment_failure_when_monitor_times_out() throws InterruptedException {
        RecordingUpdateDeploymentStatusPort updateDeploymentStatusPort = new RecordingUpdateDeploymentStatusPort();
        try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            DeploymentMonitorDomainService monitor = new DeploymentMonitorDomainService(
                    _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found(
                            ActualDeploymentState.STOPPED
                    ),
                    currentDesiredStatePort(DesiredDeploymentState.RUNNING),
                    updateDeploymentStatusPort,
                    executorService,
                    Duration.ofMillis(30),
                    Duration.ofMillis(5),
                    10,
                    false
            );

            monitor.monitorDeployment(desiredDeployment(DesiredDeploymentState.RUNNING), DEPLOYMENT_ID);

            assertThat(updateDeploymentStatusPort.awaitStatus()).isTrue();
            assertThat(updateDeploymentStatusPort.records)
                    .containsExactly(new DeploymentStatusRecord(DEPLOYMENT_ID, DeploymentStatus.FAILURE));
        }
    }

    @Test
    void marks_deployment_failure_when_docker_state_lookup_fails() throws InterruptedException {
        RecordingUpdateDeploymentStatusPort updateDeploymentStatusPort = new RecordingUpdateDeploymentStatusPort();
        try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            DeploymentMonitorDomainService monitor = new DeploymentMonitorDomainService(
                    _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Failure(),
                    currentDesiredStatePort(DesiredDeploymentState.RUNNING),
                    updateDeploymentStatusPort,
                    executorService,
                    Duration.ofSeconds(1),
                    Duration.ZERO,
                    1,
                    false
            );

            monitor.monitorDeployment(desiredDeployment(DesiredDeploymentState.RUNNING), DEPLOYMENT_ID);

            assertThat(updateDeploymentStatusPort.awaitStatus()).isTrue();
            assertThat(updateDeploymentStatusPort.records)
                    .containsExactly(new DeploymentStatusRecord(DEPLOYMENT_ID, DeploymentStatus.FAILURE));
        }
    }

    @Test
    void marks_deployment_canceled_when_desired_state_changes_during_monitoring() throws InterruptedException {
        RecordingUpdateDeploymentStatusPort updateDeploymentStatusPort = new RecordingUpdateDeploymentStatusPort();
        try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            DeploymentMonitorDomainService monitor = new DeploymentMonitorDomainService(
                    _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found(
                            ActualDeploymentState.RUNNING
                    ),
                    currentDesiredStatePort(DesiredDeploymentState.STOPPED),
                    updateDeploymentStatusPort,
                    executorService,
                    Duration.ofSeconds(1),
                    Duration.ZERO,
                    1,
                    false
            );

            monitor.monitorDeployment(desiredDeployment(DesiredDeploymentState.RUNNING), DEPLOYMENT_ID);

            assertThat(updateDeploymentStatusPort.awaitStatus()).isTrue();
            assertThat(updateDeploymentStatusPort.records)
                    .containsExactly(new DeploymentStatusRecord(DEPLOYMENT_ID, DeploymentStatus.CANCELED));
        }
    }

    @Test
    void marks_deployment_canceled_when_desired_deployment_is_removed_during_monitoring()
            throws InterruptedException {
        RecordingUpdateDeploymentStatusPort updateDeploymentStatusPort = new RecordingUpdateDeploymentStatusPort();
        try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            DeploymentMonitorDomainService monitor = new DeploymentMonitorDomainService(
                    _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found(
                            ActualDeploymentState.RUNNING
                    ),
                    _ -> new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.NotDeployed(),
                    updateDeploymentStatusPort,
                    executorService,
                    Duration.ofSeconds(1),
                    Duration.ZERO,
                    1,
                    false
            );

            monitor.monitorDeployment(desiredDeployment(DesiredDeploymentState.RUNNING), DEPLOYMENT_ID);

            assertThat(updateDeploymentStatusPort.awaitStatus()).isTrue();
            assertThat(updateDeploymentStatusPort.records)
                    .containsExactly(new DeploymentStatusRecord(DEPLOYMENT_ID, DeploymentStatus.CANCELED));
        }
    }

    @Test
    void marks_deployment_failure_when_desired_state_lookup_fails() throws InterruptedException {
        RecordingUpdateDeploymentStatusPort updateDeploymentStatusPort = new RecordingUpdateDeploymentStatusPort();
        try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            DeploymentMonitorDomainService monitor = new DeploymentMonitorDomainService(
                    _ -> new FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found(
                            ActualDeploymentState.RUNNING
                    ),
                    _ -> new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Failure(),
                    updateDeploymentStatusPort,
                    executorService,
                    Duration.ofSeconds(1),
                    Duration.ZERO,
                    1,
                    false
            );

            monitor.monitorDeployment(desiredDeployment(DesiredDeploymentState.RUNNING), DEPLOYMENT_ID);

            assertThat(updateDeploymentStatusPort.awaitStatus()).isTrue();
            assertThat(updateDeploymentStatusPort.records)
                    .containsExactly(new DeploymentStatusRecord(DEPLOYMENT_ID, DeploymentStatus.FAILURE));
        }
    }

    private static DesiredDeployment desiredDeployment(DesiredDeploymentState desiredState) {
        return new DesiredDeployment(
                SERVICE_NAME,
                new ImageRepository("ghcr.io/deployko/billing-api"),
                new ImageVersion("1.0.0"),
                RuntimeConfiguration.empty(),
                desiredState
        );
    }

    private static FindDesiredDeploymentStatePort currentDesiredStatePort(DesiredDeploymentState desiredState) {
        return _ -> new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Found(desiredState);
    }

    private record DeploymentStatusRecord(DeploymentId deploymentId, DeploymentStatus status) {
    }

    private static final class RecordingUpdateDeploymentStatusPort implements UpdateDeploymentStatusPort {

        private final CountDownLatch statusUpdated = new CountDownLatch(1);
        private final List<DeploymentStatusRecord> records = new ArrayList<>();

        @Override
        public UpdateDeploymentStatusResult updateStatus(DeploymentId deploymentId, DeploymentStatus status) {
            records.add(new DeploymentStatusRecord(deploymentId, status));
            statusUpdated.countDown();
            return new UpdateDeploymentStatusResult.Success();
        }

        private boolean awaitStatus() throws InterruptedException {
            return statusUpdated.await(1, TimeUnit.SECONDS);
        }
    }
}
