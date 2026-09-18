CREATE TABLE consumer_processed_events (
    id UUID PRIMARY KEY,
    consumer_group VARCHAR(100) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_consumer_processed_events_group_event
        UNIQUE (consumer_group, event_id)
);

CREATE INDEX idx_consumer_processed_events_processed_at
    ON consumer_processed_events (processed_at);
