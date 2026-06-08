package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.ActualDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DeploymentStatus;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;
import hr.tvz.popovic.deployko.application.port.out.FindActualDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.FindDesiredDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.UpdateDeploymentStatusPort;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DeploymentMonitorDomainService implements DeploymentMonitor, AutoCloseable {

    private static final int DEFAULT_REQUIRED_STABLE_CHECKS = 10;
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(2);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);

    private final FindActualDeploymentStatePort findActualDeploymentStatePort;
    private final FindDesiredDeploymentStatePort findDesiredDeploymentStatePort;
    private final UpdateDeploymentStatusPort updateDeploymentStatusPort;
    private final ExecutorService executorService;
    private final Duration timeout;
    private final Duration pollInterval;
    private final int requiredStableChecks;
    private final boolean closeExecutor;

    public DeploymentMonitorDomainService(
            FindActualDeploymentStatePort findActualDeploymentStatePort,
            FindDesiredDeploymentStatePort findDesiredDeploymentStatePort,
            UpdateDeploymentStatusPort updateDeploymentStatusPort
    ) {
        this(
                findActualDeploymentStatePort,
                findDesiredDeploymentStatePort,
                updateDeploymentStatusPort,
                Executors.newVirtualThreadPerTaskExecutor(),
                DEFAULT_TIMEOUT,
                DEFAULT_POLL_INTERVAL,
                DEFAULT_REQUIRED_STABLE_CHECKS,
                true
        );
    }

    DeploymentMonitorDomainService(
            FindActualDeploymentStatePort findActualDeploymentStatePort,
            FindDesiredDeploymentStatePort findDesiredDeploymentStatePort,
            UpdateDeploymentStatusPort updateDeploymentStatusPort,
            ExecutorService executorService,
            Duration timeout,
            Duration pollInterval,
            int requiredStableChecks,
            boolean closeExecutor
    ) {
        this.findActualDeploymentStatePort = Objects.requireNonNull(
                findActualDeploymentStatePort,
                "findActualDeploymentStatePort must not be null"
        );
        this.findDesiredDeploymentStatePort = Objects.requireNonNull(
                findDesiredDeploymentStatePort,
                "findDesiredDeploymentStatePort must not be null"
        );
        this.updateDeploymentStatusPort = Objects.requireNonNull(
                updateDeploymentStatusPort,
                "updateDeploymentStatusPort must not be null"
        );
        this.executorService = Objects.requireNonNull(executorService, "executorService must not be null");
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
        this.pollInterval = Objects.requireNonNull(pollInterval, "pollInterval must not be null");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        if (pollInterval.isNegative()) {
            throw new IllegalArgumentException("pollInterval must not be negative");
        }
        if (requiredStableChecks < 1) {
            throw new IllegalArgumentException("requiredStableChecks must be positive");
        }
        this.requiredStableChecks = requiredStableChecks;
        this.closeExecutor = closeExecutor;
    }

    @Override
    public void monitorDeployment(DesiredDeployment desiredDeployment, DeploymentId deploymentId) {
        Objects.requireNonNull(desiredDeployment, "desiredDeployment must not be null");
        Objects.requireNonNull(deploymentId, "deploymentId must not be null");

        executorService.submit(() -> runWithTimeout(desiredDeployment, deploymentId));
    }

    private void runWithTimeout(DesiredDeployment desiredDeployment, DeploymentId deploymentId) {
        Future<?> monitor = executorService.submit(() -> monitorUntilExpectedState(desiredDeployment, deploymentId));
        try {
            monitor.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException _) {
            monitor.cancel(true);
            updateDeploymentStatusPort.updateStatus(deploymentId, DeploymentStatus.FAILURE);
        } catch (InterruptedException _) {
            monitor.cancel(true);
            Thread.currentThread().interrupt();
        } catch (ExecutionException | CancellationException _) {
            updateDeploymentStatusPort.updateStatus(deploymentId, DeploymentStatus.FAILURE);
        }
    }

    private void monitorUntilExpectedState(DesiredDeployment desiredDeployment, DeploymentId deploymentId) {
        int stableChecks = 0;
        Integer lastRestartCount = null;
        while (stableChecks < requiredStableChecks) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            switch (findDesiredDeploymentStatePort.findDesiredState(desiredDeployment.serviceName())) {
                case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Found found
                        when found.desiredState() != desiredDeployment.desiredState() -> {
                    updateDeploymentStatusPort.updateStatus(deploymentId, DeploymentStatus.CANCELED);
                    return;
                }
                case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Found _ -> {
                }
                case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.NotDeployed _,
                     FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.ServiceNotFound _ -> {
                    updateDeploymentStatusPort.updateStatus(deploymentId, DeploymentStatus.CANCELED);
                    return;
                }
                case FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Failure _ -> {
                    updateDeploymentStatusPort.updateStatus(deploymentId, DeploymentStatus.FAILURE);
                    return;
                }
            }

            switch (findActualDeploymentStatePort.findActualState(desiredDeployment.serviceName())) {
                case FindActualDeploymentStatePort.FindActualDeploymentStateResult.Found found -> {
                    boolean restartCountChanged = lastRestartCount != null && found.restartCount() != lastRestartCount;
                    lastRestartCount = found.restartCount();
                    if (restartCountChanged) {
                        stableChecks = 0;
                    } else {
                        stableChecks = isExpectedState(desiredDeployment.desiredState(), found.actualState())
                                ? stableChecks + 1
                                : 0;
                    }
                }
                case FindActualDeploymentStatePort.FindActualDeploymentStateResult.DuplicateManagedContainers _,
                     FindActualDeploymentStatePort.FindActualDeploymentStateResult.Failure _ -> {
                    updateDeploymentStatusPort.updateStatus(deploymentId, DeploymentStatus.FAILURE);
                    return;
                }
            }

            if (stableChecks < requiredStableChecks) {
                waitForNextCheck();
            }
        }

        updateDeploymentStatusPort.updateStatus(deploymentId, DeploymentStatus.SUCCESS);
    }

    private static boolean isExpectedState(DesiredDeploymentState desiredState, ActualDeploymentState actualState) {
        return switch (desiredState) {
            case RUNNING -> actualState == ActualDeploymentState.RUNNING;
            case STOPPED ->
                    actualState == ActualDeploymentState.STOPPED || actualState == ActualDeploymentState.MISSING;
        };
    }

    private void waitForNextCheck() {
        try {
            Thread.sleep(pollInterval);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        if (closeExecutor) {
            executorService.close();
        }
    }
}
