-- Remove public share URLs and token fields from activity metadata (URLs embed plaintext tokens).
UPDATE activity_events
SET metadata = metadata - 'publicUrl' - 'publicToken'
WHERE metadata IS NOT NULL
  AND (metadata ? 'publicUrl' OR metadata ? 'publicToken');
