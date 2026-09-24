ALTER TABLE outbox_events
    DROP CONSTRAINT IF EXISTS chk_outbox_events_status;

ALTER TABLE outbox_events
    ADD CONSTRAINT chk_outbox_events_status
    CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'));

ALTER TABLE outbox_events
    ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMPTZ;

UPDATE outbox_events
SET next_attempt_at = created_at
WHERE status = 'PENDING'
  AND next_attempt_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_outbox_events_claim
    ON outbox_events (status, next_attempt_at, created_at);
