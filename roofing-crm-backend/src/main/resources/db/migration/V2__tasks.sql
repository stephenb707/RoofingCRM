-- ============================================================
-- V2__tasks.sql
-- Tasks / Follow-ups table for Roofing CRM.
-- ============================================================

CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    created_by_user_id UUID,
    updated_by_user_id UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    due_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    assigned_to_user_id UUID REFERENCES users(id),
    lead_id UUID REFERENCES leads(id),
    job_id UUID REFERENCES jobs(id),
    customer_id UUID REFERENCES customers(id)
);

CREATE INDEX idx_tasks_tenant_status ON tasks(tenant_id, status);
CREATE INDEX idx_tasks_tenant_due_at ON tasks(tenant_id, due_at);
CREATE INDEX idx_tasks_tenant_assigned_to ON tasks(tenant_id, assigned_to_user_id);
CREATE INDEX idx_tasks_tenant_lead ON tasks(tenant_id, lead_id);
CREATE INDEX idx_tasks_tenant_job ON tasks(tenant_id, job_id);
CREATE INDEX idx_tasks_tenant_customer ON tasks(tenant_id, customer_id);
CREATE INDEX idx_tasks_tenant_archived ON tasks(tenant_id, archived);
