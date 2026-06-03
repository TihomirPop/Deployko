package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface StartContainerPort {

    StartContainerResult start(ServiceName serviceName);

    sealed interface StartContainerResult
            permits StartContainerResult.Success, StartContainerResult.MissingContainer,
            StartContainerResult.DuplicateManagedContainers, StartContainerResult.Failure {

        record Success() implements StartContainerResult {
        }

        record MissingContainer() implements StartContainerResult {
        }

        record DuplicateManagedContainers() implements StartContainerResult {
        }

        record Failure() implements StartContainerResult {
        }
    }
}
