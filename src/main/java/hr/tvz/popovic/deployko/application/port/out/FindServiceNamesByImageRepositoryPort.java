package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

import java.util.List;

public interface FindServiceNamesByImageRepositoryPort {

    FindServiceNamesByImageRepositoryResult findServiceNamesByImageRepository(ImageRepository imageRepository);

    sealed interface FindServiceNamesByImageRepositoryResult
            permits FindServiceNamesByImageRepositoryResult.Found, FindServiceNamesByImageRepositoryResult.Failure {

        record Found(List<ServiceName> serviceNames) implements FindServiceNamesByImageRepositoryResult {
        }

        record Failure() implements FindServiceNamesByImageRepositoryResult {
        }
    }
}
