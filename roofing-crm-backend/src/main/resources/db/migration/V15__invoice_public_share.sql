-- Public invoice sharing
ALTER TABLE invoices
    ADD COLUMN public_token VARCHAR(64),
    ADD COLUMN public_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN public_expires_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN public_last_shared_at TIMESTAMP WITH TIME ZONE;

CREATE UNIQUE INDEX idx_invoices_public_token ON invoices(public_token) WHERE public_token IS NOT NULL;
CREATE INDEX idx_invoices_tenant_public_enabled ON invoices(tenant_id, public_enabled);
