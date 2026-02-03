-- ============================================================
-- V13__invoices.sql
-- Invoices and invoice items (snapshot from estimates).
-- ============================================================

CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    created_by_user_id UUID REFERENCES users(id),
    job_id UUID NOT NULL REFERENCES jobs(id),
    estimate_id UUID REFERENCES estimates(id),
    invoice_number VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    sent_at TIMESTAMP WITH TIME ZONE,
    due_at TIMESTAMP WITH TIME ZONE,
    paid_at TIMESTAMP WITH TIME ZONE,
    total NUMERIC(12, 2) NOT NULL,
    notes TEXT
);

CREATE INDEX idx_invoices_tenant_job ON invoices(tenant_id, job_id);
CREATE INDEX idx_invoices_tenant_status ON invoices(tenant_id, status);
CREATE UNIQUE INDEX uk_invoices_tenant_number ON invoices(tenant_id, invoice_number);

-- ============================================================
-- INVOICE_ITEMS (snapshot from estimate items at creation)
-- ============================================================

CREATE TABLE invoice_items (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    quantity NUMERIC(10, 2) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    line_total NUMERIC(12, 2) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_invoice_items_invoice_sort ON invoice_items(invoice_id, sort_order);
