alter table attachments
    add column extracted_amount_candidates_json text,
    add column extracted_amount_confidence varchar(16),
    add column extracted_warnings_json text,
    add column extracted_raw_text text;
