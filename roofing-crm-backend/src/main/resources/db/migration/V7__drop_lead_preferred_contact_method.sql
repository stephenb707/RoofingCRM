-- Preferred contact method is now on Customer only; drop from leads.
ALTER TABLE leads DROP COLUMN IF EXISTS preferred_contact_method;
