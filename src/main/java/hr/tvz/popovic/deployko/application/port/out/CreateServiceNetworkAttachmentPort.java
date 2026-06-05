package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachment;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface CreateServiceNetworkAttachmentPort {

    CreateServiceNetworkAttachmentResult createNetworkAttachment(
            ServiceName serviceName,
            NetworkAttachment networkAttachment
    );

    sealed interface CreateServiceNetworkAttachmentResult
            permits CreateServiceNetworkAttachmentResult.Created,
            CreateServiceNetworkAttachmentResult.ServiceNotFound, CreateServiceNetworkAttachmentResult.AlreadyExists,
            CreateServiceNetworkAttachmentResult.Failure {

        record Created() implements CreateServiceNetworkAttachmentResult {
        }

        record ServiceNotFound() implements CreateServiceNetworkAttachmentResult {
        }

        record AlreadyExists() implements CreateServiceNetworkAttachmentResult {
        }

        record Failure() implements CreateServiceNetworkAttachmentResult {
        }
    }
}
