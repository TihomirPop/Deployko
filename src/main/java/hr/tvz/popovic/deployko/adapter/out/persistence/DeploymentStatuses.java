package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.DeploymentStatus;

final class DeploymentStatuses {

    private DeploymentStatuses() {
    }

    static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.enums.DeploymentStatus toJooq(
            DeploymentStatus status
    ) {
        return switch (status) {
            case IN_PROGRESS ->
                    hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.enums.DeploymentStatus.IN_PROGRESS;
            case SUCCESS ->
                    hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.enums.DeploymentStatus.SUCCESS;
            case FAILURE ->
                    hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.enums.DeploymentStatus.FAILURE;
            case CANCELED ->
                    hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.enums.DeploymentStatus.CANCELED;
        };
    }

    static DeploymentStatus toDomain(
            hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.enums.DeploymentStatus status
    ) {
        return switch (status) {
            case IN_PROGRESS -> DeploymentStatus.IN_PROGRESS;
            case SUCCESS -> DeploymentStatus.SUCCESS;
            case FAILURE -> DeploymentStatus.FAILURE;
            case CANCELED -> DeploymentStatus.CANCELED;
        };
    }
}
