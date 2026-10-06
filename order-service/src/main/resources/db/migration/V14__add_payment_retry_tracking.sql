ALTER TABLE payments
    ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_retry_count
    CHECK (retry_count >= 0);

CREATE INDEX idx_payments_retry_count
    ON payments(retry_count);
