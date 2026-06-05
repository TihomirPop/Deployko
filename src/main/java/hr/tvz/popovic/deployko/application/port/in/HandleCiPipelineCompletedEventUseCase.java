package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface HandleCiPipelineCompletedEventUseCase {

    HandleCiPipelineCompletedEventResult handleCiPipelineCompletedEvent(HandleCiPipelineCompletedEventCommand command);

    record HandleCiPipelineCompletedEventCommand(
            ServiceName serviceName,
            ImageVersion imageVersion,
            long buildNumber
    ) {
    }

    sealed interface HandleCiPipelineCompletedEventResult
            permits HandleCiPipelineCompletedEventResult.Deployed,
            HandleCiPipelineCompletedEventResult.SkippedRecentDeployment,
            HandleCiPipelineCompletedEventResult.ServiceNotFound,
            HandleCiPipelineCompletedEventResult.DeploymentFailure,
            HandleCiPipelineCompletedEventResult.LastDeploymentLookupFailure,
            HandleCiPipelineCompletedEventResult.RecordDeploymentFailure {

        record Deployed() implements HandleCiPipelineCompletedEventResult {
        }

        record SkippedRecentDeployment() implements HandleCiPipelineCompletedEventResult {
        }

        record ServiceNotFound() implements HandleCiPipelineCompletedEventResult {
        }

        record DeploymentFailure() implements HandleCiPipelineCompletedEventResult {
        }

        record LastDeploymentLookupFailure() implements HandleCiPipelineCompletedEventResult {
        }

        record RecordDeploymentFailure() implements HandleCiPipelineCompletedEventResult {
        }
    }
}
