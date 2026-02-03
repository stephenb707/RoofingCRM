-- ============================================================
-- V9__tenant_invites.sql
-- Tenant invite table for team invitations.
-- ============================================================

CREATE TABLE tenant_invites (
    invite_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    token UUID NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    accepted_by_user_id UUID REFERENCES users(id),
    created_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_tenant_invites_tenant_id ON tenant_invites(tenant_id);
CREATE INDEX idx_tenant_invites_tenant_email ON tenant_invites(tenant_id, email);
CREATE INDEX idx_tenant_invites_tenant_accepted ON tenant_invites(tenant_id, accepted_at);
