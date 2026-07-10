-- Lightweight DB-backed job queue for future integrations (QuickBooks, SMS, measurements, etc.).
-- See BackgroundJobWorker for single-instance locking notes; multi-instance may need stronger coordination.

CREATE TABLE integration_background_jobs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    job_type VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    payload_json JSONB,
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 5,
    next_run_at TIMESTAMPTZ NOT NULL,
    last_error TEXT,
    locked_at TIMESTAMPTZ,
    locked_by VARCHAR(128),
    correlation_id VARCHAR(64),
    entity_type VARCHAR(64),
    entity_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_integration_bg_job_attempts_nonneg CHECK (attempts >= 0),
    CONSTRAINT chk_integration_bg_job_max_attempts_pos CHECK (max_attempts > 0)
);

CREATE INDEX idx_integration_bg_jobs_tenant ON integration_background_jobs (tenant_id);
CREATE INDEX idx_integration_bg_jobs_due ON integration_background_jobs (next_run_at)
    WHERE status = 'PENDING';
