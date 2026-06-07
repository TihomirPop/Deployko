create table service_deployment_history (
    id uuid primary key default uuidv7(),
    service_id uuid not null references services(id) on delete cascade,
    image_version text not null,
    recorded_at timestamptz not null default now()
);

create index service_deployment_history_service_recorded_at_idx
    on service_deployment_history (service_id, recorded_at desc, id desc);
