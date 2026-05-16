-- Store only SHA-256 hashes of public share tokens (estimates / invoices).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE estimates ADD COLUMN public_token_hash VARCHAR(64);
ALTER TABLE invoices ADD COLUMN public_token_hash VARCHAR(64);

UPDATE estimates
SET public_token_hash = encode(digest(public_token, 'sha256'), 'hex')
WHERE public_token IS NOT NULL AND btrim(public_token) <> '';

UPDATE invoices
SET public_token_hash = encode(digest(public_token, 'sha256'), 'hex')
WHERE public_token IS NOT NULL AND btrim(public_token) <> '';

DROP INDEX IF EXISTS idx_estimates_public_token;
DROP INDEX IF EXISTS idx_invoices_public_token;

ALTER TABLE estimates DROP COLUMN public_token;
ALTER TABLE invoices DROP COLUMN public_token;

CREATE UNIQUE INDEX idx_estimates_public_token_hash ON estimates (public_token_hash)
    WHERE public_token_hash IS NOT NULL;

CREATE UNIQUE INDEX idx_invoices_public_token_hash ON invoices (public_token_hash)
    WHERE public_token_hash IS NOT NULL;
