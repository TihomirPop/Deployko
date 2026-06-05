package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachment;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface DeleteServiceNetworkAttachmentUseCase {

    DeleteServiceNetworkAttachmentResult deleteServiceNetworkAttachment(DeleteServiceNetworkAttachmentCommand command);

    record DeleteServiceNetworkAttachmentCommand(
            ServiceName serviceName,
            NetworkAttachment.NetworkName networkName
    ) {
    }

    sealed interface DeleteServiceNetworkAttachmentResult
            permits DeleteServiceNetworkAttachmentResult.Success, DeleteServiceNetworkAttachmentResult.ServiceNotFound,
            DeleteServiceNetworkAttachmentResult.NetworkAttachmentNotFound,
            DeleteServiceNetworkAttachmentResult.Failure {

        record Success() implements DeleteServiceNetworkAttachmentResult {
        }

        record ServiceNotFound() implements DeleteServiceNetworkAttachmentResult {
        }

        record NetworkAttachmentNotFound() implements DeleteServiceNetworkAttachmentResult {
        }

        record Failure() implements DeleteServiceNetworkAttachmentResult {
        }
    }
}
