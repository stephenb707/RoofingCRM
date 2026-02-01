-- Set status to UNSCHEDULED for jobs that have no scheduled date but were marked SCHEDULED
UPDATE jobs
SET status = 'UNSCHEDULED'
WHERE scheduled_start_date IS NULL
  AND status = 'SCHEDULED';
