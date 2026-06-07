package hr.tvz.popovic.deployko.adapter.out.docker;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;

interface DockerDeploymentClient {

    void removeContainer(DesiredDeployment desiredDeployment);

    String createContainer(DesiredDeployment desiredDeployment, DeploymentId deploymentId);

    void connectToNetwork(String containerId, String networkName);

    void startContainer(String containerId);
}
