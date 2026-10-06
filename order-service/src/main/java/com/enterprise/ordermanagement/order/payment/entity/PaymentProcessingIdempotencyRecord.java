package com.enterprise.ordermanagement.order.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_processing_idempotency_records")
@Getter
@NoArgsConstructor
public class PaymentProcessingIdempotencyRecord {

    @Id
    private UUID id;

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true,
            length = 100
    )
    private String idempotencyKey;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PaymentProcessingIdempotencyRecord(
            String idempotencyKey,
            UUID paymentId
    ) {
        this.id = UUID.randomUUID();
        this.idempotencyKey = idempotencyKey;
        this.paymentId = paymentId;
    }

    @PrePersist
    protected void onCreate() {

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
