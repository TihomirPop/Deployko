package hr.tvz.popovic.deployko.application.port.in;

import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachments;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface GetServiceNetworkAttachmentsUseCase {

    GetServiceNetworkAttachmentsResult getServiceNetworkAttachments(GetServiceNetworkAttachmentsCommand command);

    record GetServiceNetworkAttachmentsCommand(ServiceName serviceName) {
    }

    sealed interface GetServiceNetworkAttachmentsResult
            permits GetServiceNetworkAttachmentsResult.Success, GetServiceNetworkAttachmentsResult.NotFound,
            GetServiceNetworkAttachmentsResult.Failure {

        record Success(NetworkAttachments networkAttachments) implements GetServiceNetworkAttachmentsResult {
        }

        record NotFound() implements GetServiceNetworkAttachmentsResult {
        }

        record Failure() implements GetServiceNetworkAttachmentsResult {
        }
    }
}
