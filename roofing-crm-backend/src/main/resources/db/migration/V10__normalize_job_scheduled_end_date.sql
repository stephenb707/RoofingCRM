-- Normalize schedule end date for one-day jobs
UPDATE jobs
SET scheduled_end_date = scheduled_start_date
WHERE scheduled_start_date IS NOT NULL
  AND scheduled_end_date IS NULL;
