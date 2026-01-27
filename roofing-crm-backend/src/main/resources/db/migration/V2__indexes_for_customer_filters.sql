-- ============================================================
-- V2__indexes_for_customer_filters.sql
-- Add indexes to support customerId filtering on leads and jobs
-- ============================================================

-- Indexes for leads filtered by customerId (and often status)
CREATE INDEX IF NOT EXISTS idx_leads_tenant_customer
  ON leads (tenant_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_leads_tenant_customer_status
  ON leads (tenant_id, customer_id, status);

-- Indexes for jobs filtered by customerId
CREATE INDEX IF NOT EXISTS idx_jobs_tenant_customer
  ON jobs (tenant_id, customer_id);

-- Common filter pattern for customers
CREATE INDEX IF NOT EXISTS idx_customers_tenant_archived
  ON customers (tenant_id, archived);
