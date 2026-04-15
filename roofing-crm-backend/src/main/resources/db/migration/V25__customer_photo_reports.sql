-- Customer-facing photo/text reports (inspection, before/after, scope summaries)

CREATE TABLE customer_photo_reports (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    created_by_user_id UUID,
    updated_by_user_id UUID,
    customer_id UUID NOT NULL REFERENCES customers(id),
    job_id UUID REFERENCES jobs(id),
    title VARCHAR(500) NOT NULL,
    report_type VARCHAR(120),
    summary TEXT
);

CREATE INDEX idx_customer_photo_reports_tenant_updated
    ON customer_photo_reports (tenant_id, updated_at DESC)
    WHERE archived = FALSE;

CREATE TABLE customer_photo_report_sections (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    report_id UUID NOT NULL REFERENCES customer_photo_reports(id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL,
    title VARCHAR(500) NOT NULL,
    body TEXT
);

CREATE INDEX idx_cprs_report_sort ON customer_photo_report_sections (report_id, sort_order);

CREATE TABLE customer_photo_report_section_photos (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    section_id UUID NOT NULL REFERENCES customer_photo_report_sections(id) ON DELETE CASCADE,
    attachment_id UUID NOT NULL REFERENCES attachments(id),
    sort_order INTEGER NOT NULL
);

CREATE INDEX idx_crsp_section_sort ON customer_photo_report_section_photos (section_id, sort_order);
