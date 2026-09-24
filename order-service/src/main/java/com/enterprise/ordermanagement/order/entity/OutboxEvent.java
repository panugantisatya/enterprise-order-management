package com.enterprise.ordermanagement.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor
public class OutboxEvent {

    public enum OutboxEventStatus {
        PENDING,
        PROCESSING,
        PUBLISHED,
        FAILED
    }

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    public OutboxEvent(
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String payload
    ) {
        this.id = UUID.randomUUID();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.retryCount = 0;
        this.status = OutboxEventStatus.PENDING;
        this.nextAttemptAt = Instant.now();
    }

    public void markProcessing() {
        this.status = OutboxEventStatus.PROCESSING;
    }

    public void markPublished() {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        this.nextAttemptAt = null;
    }

    public void markRetry(Instant nextAttemptAt) {
        this.retryCount = this.retryCount + 1;
        this.status = OutboxEventStatus.PENDING;
        this.nextAttemptAt = nextAttemptAt;
    }

    public void markFailed() {
        this.retryCount = this.retryCount + 1;
        this.status = OutboxEventStatus.FAILED;
        this.nextAttemptAt = null;
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (retryCount == null) {
            retryCount = 0;
        }

        if (status == null) {
            status = OutboxEventStatus.PENDING;
        }

        if (nextAttemptAt == null) {
            nextAttemptAt = createdAt;
        }
    }
}
