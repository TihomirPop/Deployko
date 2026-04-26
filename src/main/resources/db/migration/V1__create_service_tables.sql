create extension if not exists pgcrypto;

create table services (
    id uuid primary key default gen_random_uuid(),
    name text not null unique,
    image_repository text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table service_environment_variables (
    service_id uuid not null references services(id) on delete cascade,
    key text not null,
    value text not null,
    primary key (service_id, key)
);

create table service_port_mappings (
    service_id uuid not null references services(id) on delete cascade,
    host_port integer not null,
    host_protocol text not null,
    container_port integer not null,
    container_protocol text not null,
    primary key (service_id, host_port, host_protocol),
    unique (service_id, container_port, container_protocol)
);

create table service_volume_mounts (
    service_id uuid not null references services(id) on delete cascade,
    target_path text not null,
    mount_type text not null,
    source text not null,
    read_only boolean not null,
    primary key (service_id, target_path)
);

create table service_network_attachments (
    service_id uuid not null references services(id) on delete cascade,
    network_name text not null,
    primary key (service_id, network_name)
);
