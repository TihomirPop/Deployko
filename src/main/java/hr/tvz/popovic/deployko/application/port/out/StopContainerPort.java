package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface StopContainerPort {

    StopContainerResult stop(ServiceName serviceName);

    sealed interface StopContainerResult
            permits StopContainerResult.Success, StopContainerResult.MissingContainer,
            StopContainerResult.DuplicateManagedContainers, StopContainerResult.Failure {

        record Success() implements StopContainerResult {
        }

        record MissingContainer() implements StopContainerResult {
        }

        record DuplicateManagedContainers() implements StopContainerResult {
        }

        record Failure() implements StopContainerResult {
        }
    }
}
