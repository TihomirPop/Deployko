package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;

final class DesiredDeploymentStates {

    private DesiredDeploymentStates() {
    }

    static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.enums.DesiredDeploymentState toJooq(
            DesiredDeploymentState desiredState
    ) {
        return switch (desiredState) {
            case RUNNING ->
                    hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.enums.DesiredDeploymentState.RUNNING;
            case STOPPED ->
                    hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.enums.DesiredDeploymentState.STOPPED;
        };
    }

    static DesiredDeploymentState toDomain(
            hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.enums.DesiredDeploymentState desiredState
    ) {
        return switch (desiredState) {
            case RUNNING -> DesiredDeploymentState.RUNNING;
            case STOPPED -> DesiredDeploymentState.STOPPED;
        };
    }
}
