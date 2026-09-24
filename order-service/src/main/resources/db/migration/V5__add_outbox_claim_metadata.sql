ALTER TABLE outbox_events
    ADD COLUMN IF NOT EXISTS processing_token UUID;

ALTER TABLE outbox_events
    ADD COLUMN IF NOT EXISTS processing_started_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_outbox_events_processing_token
    ON outbox_events (processing_token);

CREATE INDEX IF NOT EXISTS idx_outbox_events_processing_started
    ON outbox_events (status, processing_started_at);
