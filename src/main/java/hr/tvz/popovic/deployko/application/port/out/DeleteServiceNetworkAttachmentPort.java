package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachment;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface DeleteServiceNetworkAttachmentPort {

    DeleteServiceNetworkAttachmentResult deleteNetworkAttachment(
            ServiceName serviceName,
            NetworkAttachment.NetworkName networkName
    );

    sealed interface DeleteServiceNetworkAttachmentResult
            permits DeleteServiceNetworkAttachmentResult.Deleted,
            DeleteServiceNetworkAttachmentResult.ServiceNotFound,
            DeleteServiceNetworkAttachmentResult.NetworkAttachmentNotFound,
            DeleteServiceNetworkAttachmentResult.Failure {

        record Deleted() implements DeleteServiceNetworkAttachmentResult {
        }

        record ServiceNotFound() implements DeleteServiceNetworkAttachmentResult {
        }

        record NetworkAttachmentNotFound() implements DeleteServiceNetworkAttachmentResult {
        }

        record Failure() implements DeleteServiceNetworkAttachmentResult {
        }
    }
}
