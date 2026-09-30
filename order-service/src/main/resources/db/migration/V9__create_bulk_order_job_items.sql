CREATE TABLE bulk_order_job_items (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    item_index INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    order_id UUID,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,

    CONSTRAINT fk_bulk_order_job_items_job
        FOREIGN KEY (job_id)
        REFERENCES bulk_order_jobs(id),

    CONSTRAINT uq_bulk_order_job_items_job_index
        UNIQUE (job_id, item_index),

    CONSTRAINT chk_bulk_order_job_items_status
        CHECK (
            status IN (
                'PROCESSING',
                'SUCCEEDED',
                'FAILED'
            )
        )
);

CREATE INDEX idx_bulk_order_job_items_job_id
    ON bulk_order_job_items(job_id);

CREATE INDEX idx_bulk_order_job_items_status
    ON bulk_order_job_items(status);
