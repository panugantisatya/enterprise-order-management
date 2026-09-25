ALTER TABLE bulk_order_jobs
    ADD COLUMN started_at TIMESTAMPTZ;

ALTER TABLE bulk_order_jobs
    ADD COLUMN completed_at TIMESTAMPTZ;

ALTER TABLE bulk_order_jobs
    ADD COLUMN error_message TEXT;
