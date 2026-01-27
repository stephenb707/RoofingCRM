-- ============================================================
-- V1__initial_schema.sql
-- Baseline schema for Roofing CRM (squashed from legacy V1–V4).
-- DB reset required if upgrading from pre-squash migrations.
-- ============================================================

-- ============================================================
-- TENANTS
-- ============================================================
CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) UNIQUE,
    default_currency_code VARCHAR(3),
    time_zone VARCHAR(64),
    locale VARCHAR(10)
);

CREATE INDEX idx_tenant_slug ON tenants(slug);

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    full_name VARCHAR(255)
);

CREATE INDEX idx_user_email ON users(email);

-- ============================================================
-- TENANT_USER_MEMBERSHIPS
-- ============================================================
CREATE TABLE tenant_user_memberships (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL REFERENCES users(id),
    role VARCHAR(50) NOT NULL,
    CONSTRAINT uk_tenant_user UNIQUE (tenant_id, user_id)
);

CREATE INDEX idx_membership_user ON tenant_user_memberships(user_id);
CREATE INDEX idx_membership_tenant_role ON tenant_user_memberships(tenant_id, role);

-- ============================================================
-- CUSTOMERS
-- ============================================================
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    created_by_user_id UUID,
    updated_by_user_id UUID,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    primary_phone VARCHAR(255),
    email VARCHAR(255),
    billing_address_line1 VARCHAR(255),
    billing_address_line2 VARCHAR(255),
    billing_city VARCHAR(255),
    billing_state VARCHAR(255),
    billing_zip VARCHAR(255),
    billing_country_code VARCHAR(255),
    notes TEXT
);

CREATE INDEX idx_customer_tenant_last_name ON customers(tenant_id, last_name);
CREATE INDEX idx_customer_tenant_email ON customers(tenant_id, email);
CREATE INDEX idx_customers_tenant_archived ON customers(tenant_id, archived);

-- ============================================================
-- LEADS
-- ============================================================
CREATE TABLE leads (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    created_by_user_id UUID,
    updated_by_user_id UUID,
    customer_id UUID REFERENCES customers(id),
    status VARCHAR(50) NOT NULL,
    source VARCHAR(50) NOT NULL,
    lead_notes TEXT,
    property_address_line1 VARCHAR(255),
    property_address_line2 VARCHAR(255),
    property_city VARCHAR(255),
    property_state VARCHAR(255),
    property_zip VARCHAR(255),
    property_country_code VARCHAR(255),
    preferred_contact_method VARCHAR(255)
);

CREATE INDEX idx_lead_tenant_status ON leads(tenant_id, status);
CREATE INDEX idx_lead_tenant_source ON leads(tenant_id, source);
CREATE INDEX idx_leads_tenant_customer ON leads(tenant_id, customer_id);
CREATE INDEX idx_leads_tenant_customer_status ON leads(tenant_id, customer_id, status);

-- ============================================================
-- JOBS
-- ============================================================
CREATE TABLE jobs (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    created_by_user_id UUID,
    updated_by_user_id UUID,
    customer_id UUID NOT NULL REFERENCES customers(id),
    lead_id UUID REFERENCES leads(id),
    status VARCHAR(50) NOT NULL,
    job_type VARCHAR(50) NOT NULL,
    roof_type VARCHAR(255),
    scheduled_start_date DATE,
    scheduled_end_date DATE,
    actual_start_date DATE,
    actual_end_date DATE,
    assigned_crew VARCHAR(255),
    job_notes TEXT,
    job_property_address_line1 VARCHAR(255),
    job_property_address_line2 VARCHAR(255),
    job_property_city VARCHAR(255),
    job_property_state VARCHAR(255),
    job_property_zip VARCHAR(255),
    job_property_country_code VARCHAR(255)
);

CREATE INDEX idx_job_tenant_status ON jobs(tenant_id, status);
CREATE INDEX idx_job_tenant_customer ON jobs(tenant_id, customer_id);
CREATE UNIQUE INDEX ux_jobs_tenant_lead ON jobs (tenant_id, lead_id) WHERE lead_id IS NOT NULL;

-- ============================================================
-- ESTIMATES
-- ============================================================
CREATE TABLE estimates (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    created_by_user_id UUID,
    updated_by_user_id UUID,
    job_id UUID NOT NULL REFERENCES jobs(id),
    estimate_number VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    issue_date DATE,
    valid_until DATE,
    subtotal NUMERIC(12, 2),
    tax NUMERIC(12, 2),
    total NUMERIC(12, 2),
    notes_for_customer TEXT,
    internal_notes TEXT,
    CONSTRAINT uk_estimate_tenant_number UNIQUE (tenant_id, estimate_number)
);

CREATE INDEX idx_estimate_tenant_job ON estimates(tenant_id, job_id);

-- ============================================================
-- ESTIMATE_ITEMS
-- ============================================================
CREATE TABLE estimate_items (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    created_by_user_id UUID,
    updated_by_user_id UUID,
    estimate_id UUID NOT NULL REFERENCES estimates(id),
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    quantity NUMERIC(10, 2) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    line_total NUMERIC(12, 2) NOT NULL,
    unit VARCHAR(50),
    sort_order INTEGER
);

CREATE INDEX idx_estimate_item_tenant_estimate ON estimate_items(tenant_id, estimate_id);

-- ============================================================
-- ATTACHMENTS
-- ============================================================
CREATE TABLE attachments (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    created_by_user_id UUID,
    updated_by_user_id UUID,
    lead_id UUID REFERENCES leads(id),
    job_id UUID REFERENCES jobs(id),
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_provider VARCHAR(50) NOT NULL,
    storage_key VARCHAR(500),
    external_provider VARCHAR(255),
    external_asset_id VARCHAR(255),
    description TEXT
);

CREATE INDEX idx_attachment_tenant_lead ON attachments(tenant_id, lead_id);
CREATE INDEX idx_attachment_tenant_job ON attachments(tenant_id, job_id);

-- ============================================================
-- COMMUNICATION_LOGS
-- ============================================================
CREATE TABLE communication_logs (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    created_by_user_id UUID,
    updated_by_user_id UUID,
    lead_id UUID REFERENCES leads(id),
    job_id UUID REFERENCES jobs(id),
    channel VARCHAR(20) NOT NULL,
    direction VARCHAR(20),
    subject VARCHAR(255),
    body TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    external_id VARCHAR(255),
    external_provider VARCHAR(255)
);

CREATE INDEX idx_comm_tenant_lead_occurred ON communication_logs(tenant_id, lead_id, occurred_at);
CREATE INDEX idx_comm_tenant_job_occurred ON communication_logs(tenant_id, job_id, occurred_at);
CREATE INDEX idx_comm_tenant_channel ON communication_logs(tenant_id, channel);
