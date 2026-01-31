-- ============================================================
-- V3__activity_events.sql
-- Activity timeline events for Leads and Jobs.
-- ============================================================

CREATE TABLE activity_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    entity_type VARCHAR(32) NOT NULL,
    entity_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    message TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by_user_id UUID REFERENCES users(id),
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_activity_tenant_entity ON activity_events(tenant_id, entity_type, entity_id, created_at DESC);
CREATE INDEX idx_activity_tenant_event_type ON activity_events(tenant_id, event_type);
CREATE INDEX idx_activity_tenant_archived ON activity_events(tenant_id, archived);
