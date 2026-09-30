ALTER TABLE bulk_order_job_items
    ADD COLUMN processing_started_at TIMESTAMPTZ;

CREATE INDEX idx_bulk_order_job_items_processing_started
    ON bulk_order_job_items(status, processing_started_at);
