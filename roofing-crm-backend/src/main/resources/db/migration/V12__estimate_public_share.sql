-- Public estimate sharing
ALTER TABLE estimates
    ADD COLUMN public_token VARCHAR(64),
    ADD COLUMN public_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN public_expires_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN public_last_shared_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN decision_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN decision_by_name VARCHAR(255),
    ADD COLUMN decision_by_email VARCHAR(255);

CREATE UNIQUE INDEX idx_estimates_public_token ON estimates(public_token) WHERE public_token IS NOT NULL;

CREATE INDEX idx_estimates_tenant_public_enabled ON estimates(tenant_id, public_enabled);
