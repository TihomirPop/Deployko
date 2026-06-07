package hr.tvz.popovic.deployko.adapter.out.docker;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentId;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;
import java.util.LinkedHashMap;
import java.util.Map;

final class DockerDeploymentMetadata {

    static final String MANAGED_LABEL = "deployko.managed";
    static final String MANAGED_LABEL_VALUE = "true";
    static final String SERVICE_NAME_LABEL = "deployko.service.name";
    static final String IMAGE_REPOSITORY_LABEL = "deployko.image.repository";
    static final String IMAGE_VERSION_LABEL = "deployko.image.version";
    static final String DEPLOYMENT_ID_LABEL = "deployko.deployment.id";

    private DockerDeploymentMetadata() {
    }

    static String imageReference(DesiredDeployment desiredDeployment) {
        return desiredDeployment.imageRepository().value() + ":" + desiredDeployment.imageVersion().value();
    }

    static String containerName(DesiredDeployment desiredDeployment) {
        return "deployko-" + desiredDeployment.serviceName().value();
    }

    static Map<String, String> labels(DesiredDeployment desiredDeployment, DeploymentId deploymentId) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(MANAGED_LABEL, MANAGED_LABEL_VALUE);
        labels.put(SERVICE_NAME_LABEL, desiredDeployment.serviceName().value());
        labels.put(IMAGE_REPOSITORY_LABEL, desiredDeployment.imageRepository().value());
        labels.put(IMAGE_VERSION_LABEL, desiredDeployment.imageVersion().value());
        labels.put(DEPLOYMENT_ID_LABEL, deploymentId.value().toString());
        return Map.copyOf(labels);
    }
}
