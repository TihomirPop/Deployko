package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface RemoveContainerPort {

    RemoveContainerResult remove(ServiceName serviceName);

    sealed interface RemoveContainerResult
            permits RemoveContainerResult.Success, RemoveContainerResult.MissingContainer,
            RemoveContainerResult.DuplicateManagedContainers, RemoveContainerResult.Failure {

        record Success() implements RemoveContainerResult {
        }

        record MissingContainer() implements RemoveContainerResult {
        }

        record DuplicateManagedContainers() implements RemoveContainerResult {
        }

        record Failure() implements RemoveContainerResult {
        }
    }
}
