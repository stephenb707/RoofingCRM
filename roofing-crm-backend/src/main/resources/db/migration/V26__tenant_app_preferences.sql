CREATE TABLE tenant_app_preferences (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    preferences   JSONB NOT NULL DEFAULT '{}',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_tenant_app_preferences_tenant UNIQUE (tenant_id)
);

CREATE INDEX idx_tenant_app_preferences_tenant ON tenant_app_preferences(tenant_id);
