-- Tenant-configurable pipeline status definitions for leads and jobs.

CREATE TABLE pipeline_status_definitions (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    created_by_user_id UUID,
    updated_by_user_id UUID,
    pipeline_type VARCHAR(10) NOT NULL,
    system_key VARCHAR(64) NOT NULL,
    label VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,
    built_in BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_pipeline_type CHECK (pipeline_type IN ('LEAD', 'JOB')),
    CONSTRAINT uk_pipeline_status_def_tenant_type_key UNIQUE (tenant_id, pipeline_type, system_key)
);

CREATE INDEX idx_psd_tenant_type_active ON pipeline_status_definitions(tenant_id, pipeline_type, active);

-- Seed defaults per tenant (LEAD)
INSERT INTO pipeline_status_definitions (
    id, created_at, updated_at, archived, tenant_id, pipeline_type, system_key, label, sort_order, built_in, active
)
SELECT gen_random_uuid(), NOW(), NOW(), FALSE, t.id, 'LEAD', v.sk, v.lb, v.ord, TRUE, TRUE
FROM tenants t
CROSS JOIN (VALUES
    ('NEW', 'New', 0),
    ('CONTACTED', 'Contacted', 1),
    ('INSPECTION_SCHEDULED', 'Inspection Scheduled', 2),
    ('QUOTE_SENT', 'Quote Sent', 3),
    ('WON', 'Won', 4),
    ('LOST', 'Lost', 5)
) AS v(sk, lb, ord);

-- Seed defaults per tenant (JOB)
INSERT INTO pipeline_status_definitions (
    id, created_at, updated_at, archived, tenant_id, pipeline_type, system_key, label, sort_order, built_in, active
)
SELECT gen_random_uuid(), NOW(), NOW(), FALSE, t.id, 'JOB', v.sk, v.lb, v.ord, TRUE, TRUE
FROM tenants t
CROSS JOIN (VALUES
    ('UNSCHEDULED', 'Unscheduled', 0),
    ('SCHEDULED', 'Scheduled', 1),
    ('IN_PROGRESS', 'In Progress', 2),
    ('COMPLETED', 'Completed', 3),
    ('INVOICED', 'Invoiced', 4)
) AS v(sk, lb, ord);

-- Leads: add FK, backfill, drop legacy status column
ALTER TABLE leads ADD COLUMN status_definition_id UUID REFERENCES pipeline_status_definitions(id);

UPDATE leads l
SET status_definition_id = psd.id
FROM pipeline_status_definitions psd
WHERE psd.tenant_id = l.tenant_id
  AND psd.pipeline_type = 'LEAD'
  AND psd.system_key = l.status;

ALTER TABLE leads ALTER COLUMN status_definition_id SET NOT NULL;

DROP INDEX IF EXISTS idx_lead_tenant_status;
DROP INDEX IF EXISTS idx_leads_tenant_customer_status;
DROP INDEX IF EXISTS idx_leads_tenant_status_position;

ALTER TABLE leads DROP COLUMN status;

CREATE INDEX idx_lead_tenant_status_def ON leads(tenant_id, status_definition_id);
CREATE INDEX idx_leads_tenant_status_def_position
    ON leads(tenant_id, status_definition_id, pipeline_position)
    WHERE archived = false;

-- Jobs: add FK, backfill, drop legacy status column
ALTER TABLE jobs ADD COLUMN status_definition_id UUID REFERENCES pipeline_status_definitions(id);

UPDATE jobs j
SET status_definition_id = psd.id
FROM pipeline_status_definitions psd
WHERE psd.tenant_id = j.tenant_id
  AND psd.pipeline_type = 'JOB'
  AND psd.system_key = j.status;

ALTER TABLE jobs ALTER COLUMN status_definition_id SET NOT NULL;

DROP INDEX IF EXISTS idx_job_tenant_status;

ALTER TABLE jobs DROP COLUMN status;

CREATE INDEX idx_job_tenant_status_def ON jobs(tenant_id, status_definition_id);
