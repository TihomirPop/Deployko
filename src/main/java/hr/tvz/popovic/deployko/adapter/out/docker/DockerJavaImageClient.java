package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.ContainerConfig;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

final class DockerJavaImageClient {

    private final DockerClient dockerClient;

    DockerJavaImageClient(DockerClient dockerClient) {
        this.dockerClient = Objects.requireNonNull(dockerClient, "dockerClient must not be null");
    }

    public void pullImage(String imageReference) {
        try {
            dockerClient.pullImageCmd(imageReference)
                    .exec(new PullImageResultCallback())
                    .awaitCompletion(5, TimeUnit.MINUTES);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DockerException("Interrupted while pulling image " + imageReference, 500, exception);
        }
    }

    public Map<String, String> imageLabels(String imageReference) {
        InspectImageResponse image = dockerClient.inspectImageCmd(imageReference).exec();
        ContainerConfig config = image.getConfig();
        if (config == null || config.getLabels() == null) {
            return Map.of();
        }
        return config.getLabels();
    }
}
