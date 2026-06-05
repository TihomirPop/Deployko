create table service_ci_deployments (
    service_id uuid primary key references services(id) on delete cascade,
    last_deployed_at timestamptz not null
);
