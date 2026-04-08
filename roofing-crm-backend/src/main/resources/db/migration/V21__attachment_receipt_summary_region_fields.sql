alter table attachments
    add column summary_region_subtotal numeric(12,2),
    add column summary_region_tax numeric(12,2),
    add column summary_region_total numeric(12,2),
    add column summary_region_amount_paid numeric(12,2),
    add column summary_region_raw_text text;
