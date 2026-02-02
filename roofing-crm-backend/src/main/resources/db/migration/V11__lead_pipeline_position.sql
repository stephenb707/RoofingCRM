-- Add pipeline position for Kanban ordering per (tenant, status)
ALTER TABLE leads
ADD COLUMN pipeline_position INT NOT NULL DEFAULT 0;

-- Backfill with stable order per (tenant_id, status) based on created_at
WITH numbered AS (
  SELECT id,
         row_number() OVER (PARTITION BY tenant_id, status ORDER BY created_at, id) - 1 AS rn
  FROM leads
  WHERE archived = false
)
UPDATE leads l
SET pipeline_position = n.rn
FROM numbered n
WHERE l.id = n.id;

-- Index for board loads (active leads by tenant, status, position)
CREATE INDEX idx_leads_tenant_status_position
ON leads(tenant_id, status, pipeline_position)
WHERE archived = false;
