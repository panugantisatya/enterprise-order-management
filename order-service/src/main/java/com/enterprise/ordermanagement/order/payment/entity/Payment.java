package com.enterprise.ordermanagement.order.payment.entity;

import com.enterprise.ordermanagement.order.exception.InvalidPaymentStatusTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor
public class Payment {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_payment_id", length = 100)
    private String providerPaymentId;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public Payment(
            UUID orderId,
            UUID customerId,
            BigDecimal amount,
            String currency,
            String provider
    ) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.provider = provider;
        this.status = PaymentStatus.PENDING;
    }

    public void startProcessing() {
        if (status != PaymentStatus.PENDING) {
            throw invalidTransition(PaymentStatus.PROCESSING);
        }

        status = PaymentStatus.PROCESSING;
        failureReason = null;
    }

    public void markSucceeded(String providerPaymentId) {
        if (status != PaymentStatus.PROCESSING) {
            throw invalidTransition(PaymentStatus.SUCCEEDED);
        }

        status = PaymentStatus.SUCCEEDED;
        this.providerPaymentId = providerPaymentId;
        this.failureReason = null;
        this.completedAt = Instant.now();
    }

    public void markFailed(String failureReason) {
        if (status != PaymentStatus.PROCESSING) {
            throw invalidTransition(PaymentStatus.FAILED);
        }

        status = PaymentStatus.FAILED;
        this.failureReason = failureReason;
        this.completedAt = Instant.now();
    }

    public void cancel() {
        if (status != PaymentStatus.PENDING) {
            throw invalidTransition(PaymentStatus.CANCELLED);
        }

        status = PaymentStatus.CANCELLED;
        completedAt = Instant.now();
    }

    private InvalidPaymentStatusTransitionException invalidTransition(
            PaymentStatus target
    ) {
        return new InvalidPaymentStatusTransitionException(
                "Invalid payment status transition from "
                        + status
                        + " to "
                        + target
        );
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
