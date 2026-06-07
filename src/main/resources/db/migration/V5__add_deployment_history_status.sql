alter table service_deployment_history
    add column status text not null default 'IN_PROGRESS';

alter table service_deployment_history
    add constraint service_deployment_history_status_check
        check (status in ('IN_PROGRESS', 'SUCCESS', 'FAILURE', 'CANCELED'));
