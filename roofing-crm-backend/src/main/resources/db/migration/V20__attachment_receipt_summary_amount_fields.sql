alter table attachments
    add column extracted_subtotal numeric(12,2),
    add column extracted_tax numeric(12,2),
    add column extracted_total numeric(12,2),
    add column extracted_amount_paid numeric(12,2);
