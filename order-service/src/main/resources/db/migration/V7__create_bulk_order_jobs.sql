CREATE TABLE bulk_order_jobs (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_items INTEGER NOT NULL,
    processed_items INTEGER NOT NULL DEFAULT 0,
    succeeded_items INTEGER NOT NULL DEFAULT 0,
    failed_items INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_bulk_order_jobs_idempotency_key
        UNIQUE (idempotency_key),

    CONSTRAINT chk_bulk_order_jobs_status
        CHECK (
            status IN (
                'ACCEPTED',
                'PROCESSING',
                'COMPLETED',
                'COMPLETED_WITH_ERRORS',
                'FAILED'
            )
        ),

    CONSTRAINT chk_bulk_order_jobs_total_items
        CHECK (total_items > 0),

    CONSTRAINT chk_bulk_order_jobs_processed_items
        CHECK (processed_items >= 0),

    CONSTRAINT chk_bulk_order_jobs_succeeded_items
        CHECK (succeeded_items >= 0),

    CONSTRAINT chk_bulk_order_jobs_failed_items
        CHECK (failed_items >= 0)
);

CREATE INDEX idx_bulk_order_jobs_status
    ON bulk_order_jobs(status);

CREATE INDEX idx_bulk_order_jobs_created_at
    ON bulk_order_jobs(created_at);
