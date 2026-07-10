-- Tenant-level integration credentials and non-secret configuration placeholders.
-- Secrets are stored encrypted via IntegrationSecretProtector (see APP_INTEGRATIONS_ENCRYPTION_KEY).

CREATE TABLE tenant_integration_connections (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    provider VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    display_name VARCHAR(255),
    config_json JSONB,
    encrypted_secret_json TEXT,
    last_connected_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_tenant_integration_provider UNIQUE (tenant_id, provider)
);

CREATE INDEX idx_tenant_integration_connections_tenant ON tenant_integration_connections (tenant_id);
