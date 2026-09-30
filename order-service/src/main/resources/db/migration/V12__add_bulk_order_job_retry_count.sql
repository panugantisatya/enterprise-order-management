ALTER TABLE bulk_order_jobs
    ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE bulk_order_jobs
    ADD CONSTRAINT chk_bulk_order_jobs_retry_count_non_negative
    CHECK (retry_count >= 0);
