CREATE TABLE payment_processing_idempotency_records (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    payment_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_payment_processing_idempotency_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id)
);

CREATE INDEX idx_payment_processing_idempotency_payment_id
    ON payment_processing_idempotency_records(payment_id);

CREATE INDEX idx_payment_processing_idempotency_created_at
    ON payment_processing_idempotency_records(created_at);
