-- ============================================================
-- V3__drop_redundant_jobs_customer_index.sql
-- Drop the redundant index created in V2 (idx_jobs_tenant_customer)
-- V1 already has idx_job_tenant_customer on the same columns
-- ============================================================

DROP INDEX IF EXISTS idx_jobs_tenant_customer;
