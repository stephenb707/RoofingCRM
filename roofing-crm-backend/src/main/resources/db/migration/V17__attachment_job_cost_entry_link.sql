alter table attachments
    add column job_cost_entry_id uuid references job_cost_entries(id);

create index idx_attachment_job_cost_entry on attachments(job_cost_entry_id);
