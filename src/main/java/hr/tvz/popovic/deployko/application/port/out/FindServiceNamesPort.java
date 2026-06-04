package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import java.util.List;

public interface FindServiceNamesPort {

    FindServiceNamesResult findServiceNames();

    sealed interface FindServiceNamesResult permits FindServiceNamesResult.Found, FindServiceNamesResult.Failure {

        record Found(List<ServiceName> serviceNames) implements FindServiceNamesResult {
        }

        record Failure() implements FindServiceNamesResult {
        }
    }
}
