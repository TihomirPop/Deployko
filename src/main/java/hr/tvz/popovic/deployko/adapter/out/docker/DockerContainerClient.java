package hr.tvz.popovic.deployko.adapter.out.docker;

import com.github.dockerjava.api.model.Container;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;

import java.util.List;

interface DockerContainerClient {

    List<Container> listManagedContainers(ServiceName serviceName);

    void startContainer(String containerId);

    void stopContainer(String containerId);

    void removeContainer(String containerId);
}
