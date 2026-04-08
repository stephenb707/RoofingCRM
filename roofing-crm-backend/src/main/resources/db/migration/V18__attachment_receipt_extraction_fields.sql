alter table attachments
    add column extraction_status varchar(32) not null default 'NOT_STARTED',
    add column extracted_at timestamptz,
    add column extraction_error varchar(255),
    add column extracted_vendor_name varchar(255),
    add column extracted_incurred_at timestamptz,
    add column extracted_amount numeric(12,2),
    add column extracted_suggested_category varchar(32),
    add column extracted_notes text,
    add column extraction_confidence integer;

create index idx_attachment_extraction_status on attachments(extraction_status);
