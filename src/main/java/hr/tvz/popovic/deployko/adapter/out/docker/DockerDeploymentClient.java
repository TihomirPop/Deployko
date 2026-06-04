package hr.tvz.popovic.deployko.adapter.out.docker;

import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;

interface DockerDeploymentClient {

    void removeContainer(DesiredDeployment desiredDeployment);

    String createContainer(DesiredDeployment desiredDeployment);

    void connectToNetwork(String containerId, String networkName);

    void startContainer(String containerId);
}
