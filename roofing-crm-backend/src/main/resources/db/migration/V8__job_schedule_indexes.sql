-- Indexes to support schedule queries (tenant + date/crew filters)
CREATE INDEX IF NOT EXISTS idx_jobs_tenant_sched_start ON jobs(tenant_id, scheduled_start_date);
CREATE INDEX IF NOT EXISTS idx_jobs_tenant_sched_end   ON jobs(tenant_id, scheduled_end_date);
CREATE INDEX IF NOT EXISTS idx_jobs_tenant_crew        ON jobs(tenant_id, assigned_crew);
