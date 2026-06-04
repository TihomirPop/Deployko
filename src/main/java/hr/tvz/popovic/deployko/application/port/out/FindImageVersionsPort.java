package hr.tvz.popovic.deployko.application.port.out;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;

import java.util.List;

public interface FindImageVersionsPort {

    FindImageVersionsResult findImageVersions(ImageRepository imageRepository);

    sealed interface FindImageVersionsResult
            permits FindImageVersionsResult.Found, FindImageVersionsResult.Failure {

        record Found(List<ImageVersion> imageVersions) implements FindImageVersionsResult {
        }

        record Failure() implements FindImageVersionsResult {
        }
    }
}
