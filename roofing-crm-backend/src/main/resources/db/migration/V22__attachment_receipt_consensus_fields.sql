alter table attachments
    add column computed_total numeric(12,2),
    add column subtotal_confidence varchar(16),
    add column tax_confidence varchar(16),
    add column total_confidence varchar(16),
    add column amount_paid_confidence varchar(16);
