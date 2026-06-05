package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachment;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface CreateServiceNetworkAttachmentUseCase {

    CreateServiceNetworkAttachmentResult createServiceNetworkAttachment(CreateServiceNetworkAttachmentCommand command);

    record CreateServiceNetworkAttachmentCommand(ServiceName serviceName, NetworkAttachment networkAttachment) {
    }

    sealed interface CreateServiceNetworkAttachmentResult
            permits CreateServiceNetworkAttachmentResult.Success, CreateServiceNetworkAttachmentResult.ServiceNotFound,
            CreateServiceNetworkAttachmentResult.AlreadyExists, CreateServiceNetworkAttachmentResult.Failure {

        record Success() implements CreateServiceNetworkAttachmentResult {
        }

        record ServiceNotFound() implements CreateServiceNetworkAttachmentResult {
        }

        record AlreadyExists() implements CreateServiceNetworkAttachmentResult {
        }

        record Failure() implements CreateServiceNetworkAttachmentResult {
        }
    }
}
