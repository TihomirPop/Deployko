package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface FindServiceSummaryCandidatesPort {

    FindServiceSummaryCandidatesResult findServiceSummaryCandidates();

    record ServiceSummaryCandidate(
            ServiceName name,
            ImageRepository imageRepository,
            Optional<ImageVersion> deployedVersion,
            Optional<DesiredDeploymentState> desiredState
    ) {

        public ServiceSummaryCandidate {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(imageRepository, "imageRepository must not be null");
            deployedVersion = Objects.requireNonNull(deployedVersion, "deployedVersion must not be null");
            desiredState = Objects.requireNonNull(desiredState, "desiredState must not be null");
        }
    }

    sealed interface FindServiceSummaryCandidatesResult
            permits FindServiceSummaryCandidatesResult.Found, FindServiceSummaryCandidatesResult.Failure {

        record Found(List<ServiceSummaryCandidate> services) implements FindServiceSummaryCandidatesResult {
        }

        record Failure() implements FindServiceSummaryCandidatesResult {
        }
    }
}
