-- ============================================================
-- V14__invoice_audit_columns.sql
-- Add missing updated_by_user_id to invoices (expected by TenantAuditedEntity).
-- ============================================================

ALTER TABLE invoices ADD COLUMN IF NOT EXISTS updated_by_user_id UUID REFERENCES users(id);
