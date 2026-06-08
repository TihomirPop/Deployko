package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;

public interface DeploymentMonitor {

    void monitorDeployment(DesiredDeployment desiredDeployment, DeploymentId deploymentId);
}
