package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;

public interface HandleCiPipelineCompletedEventUseCase {

    HandleCiPipelineCompletedEventResult handleCiPipelineCompletedEvent(HandleCiPipelineCompletedEventCommand command);

    record HandleCiPipelineCompletedEventCommand(
            ImageRepository imageRepository,
            ImageVersion imageVersion,
            long buildNumber
    ) {
    }

    sealed interface HandleCiPipelineCompletedEventResult
            permits HandleCiPipelineCompletedEventResult.Deployed,
            HandleCiPipelineCompletedEventResult.SkippedRecentDeployment,
            HandleCiPipelineCompletedEventResult.NoMatchingServices,
            HandleCiPipelineCompletedEventResult.DeploymentFailure,
            HandleCiPipelineCompletedEventResult.ServiceLookupFailure,
            HandleCiPipelineCompletedEventResult.LastDeploymentLookupFailure,
            HandleCiPipelineCompletedEventResult.RecordDeploymentFailure {

        record Deployed() implements HandleCiPipelineCompletedEventResult {
        }

        record SkippedRecentDeployment() implements HandleCiPipelineCompletedEventResult {
        }

        record NoMatchingServices() implements HandleCiPipelineCompletedEventResult {
        }

        record ServiceLookupFailure() implements HandleCiPipelineCompletedEventResult {
        }

        record DeploymentFailure() implements HandleCiPipelineCompletedEventResult {
        }

        record LastDeploymentLookupFailure() implements HandleCiPipelineCompletedEventResult {
        }

        record RecordDeploymentFailure() implements HandleCiPipelineCompletedEventResult {
        }
    }
}
