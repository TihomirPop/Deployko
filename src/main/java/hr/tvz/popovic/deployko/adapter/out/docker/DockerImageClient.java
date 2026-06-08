package hr.tvz.popovic.deployko.adapter.out.docker;

import java.util.Map;

interface DockerImageClient {

    void pullImage(String imageReference);

    Map<String, String> imageLabels(String imageReference);
}
