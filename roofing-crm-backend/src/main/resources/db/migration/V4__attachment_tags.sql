-- Add tag column for attachment categorization
ALTER TABLE attachments
    ADD COLUMN tag VARCHAR(32) NOT NULL DEFAULT 'OTHER';

-- Indexes for common queries by lead/job + tag
CREATE INDEX idx_attachment_tenant_lead_tag ON attachments(tenant_id, lead_id, tag) WHERE lead_id IS NOT NULL;
CREATE INDEX idx_attachment_tenant_job_tag ON attachments(tenant_id, job_id, tag) WHERE job_id IS NOT NULL;
