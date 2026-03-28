create table job_cost_entries (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    job_id uuid not null references jobs(id),
    category varchar(32) not null,
    vendor_name varchar(255),
    description varchar(255) not null,
    amount numeric(12,2) not null,
    incurred_at timestamptz not null,
    notes text,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    archived boolean not null default false,
    archived_at timestamptz,
    created_by_user_id uuid,
    updated_by_user_id uuid,
    constraint chk_job_cost_entries_amount_nonnegative check (amount >= 0)
);

create index idx_job_cost_entries_tenant_id on job_cost_entries(tenant_id);
create index idx_job_cost_entries_job_id on job_cost_entries(job_id);
create index idx_job_cost_entries_category on job_cost_entries(category);
create index idx_job_cost_entries_incurred_at on job_cost_entries(incurred_at);
