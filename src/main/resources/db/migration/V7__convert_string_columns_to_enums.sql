create type volume_mount_type as enum ('BIND', 'VOLUME');
create type port_protocol as enum ('TCP', 'UDP');
create type desired_deployment_state as enum ('RUNNING', 'STOPPED');
create type deployment_status as enum ('IN_PROGRESS', 'SUCCESS', 'FAILURE', 'CANCELED');

alter table service_volume_mounts
    alter column mount_type type volume_mount_type using mount_type::volume_mount_type;

alter table service_desired_deployment_volume_mounts
    alter column mount_type type volume_mount_type using mount_type::volume_mount_type;

alter table service_port_mappings
    alter column host_protocol type port_protocol using host_protocol::port_protocol,
    alter column container_protocol type port_protocol using container_protocol::port_protocol;

alter table service_desired_deployment_port_mappings
    alter column host_protocol type port_protocol using host_protocol::port_protocol,
    alter column container_protocol type port_protocol using container_protocol::port_protocol;

alter table service_desired_deployments
    drop constraint service_desired_deployments_desired_state_check;

alter table service_desired_deployments
    alter column desired_state type desired_deployment_state using desired_state::desired_deployment_state;

alter table service_deployment_history
    alter column status drop default;

alter table service_deployment_history
    drop constraint service_deployment_history_status_check;

alter table service_deployment_history
    alter column status type deployment_status using status::deployment_status;

alter table service_deployment_history
    alter column status set default 'IN_PROGRESS'::deployment_status;
