package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachments;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

public interface FindServiceNetworkAttachmentsPort {

    FindServiceNetworkAttachmentsResult findNetworkAttachments(ServiceName serviceName);

    sealed interface FindServiceNetworkAttachmentsResult
            permits FindServiceNetworkAttachmentsResult.Found, FindServiceNetworkAttachmentsResult.ServiceNotFound,
            FindServiceNetworkAttachmentsResult.Failure {

        record Found(NetworkAttachments networkAttachments) implements FindServiceNetworkAttachmentsResult {
        }

        record ServiceNotFound() implements FindServiceNetworkAttachmentsResult {
        }

        record Failure() implements FindServiceNetworkAttachmentsResult {
        }
    }
}
