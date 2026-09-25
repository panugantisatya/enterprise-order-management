package com.enterprise.ordermanagement.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bulk_order_jobs")
@Getter
@NoArgsConstructor
public class BulkOrderJob {

    public enum BulkOrderJobStatus {
        ACCEPTED,
        PROCESSING,
        COMPLETED,
        COMPLETED_WITH_ERRORS,
        FAILED
    }

    @Id
    private UUID id;

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true,
            length = 100
    )
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BulkOrderJobStatus status;

    @Column(name = "total_items", nullable = false)
    private Integer totalCount;

    @Column(name = "processed_items", nullable = false)
    private Integer processedCount;

    @Column(name = "succeeded_items", nullable = false)
    private Integer succeededCount;

    @Column(name = "failed_items", nullable = false)
    private Integer failedCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public BulkOrderJob(
            String idempotencyKey,
            int totalCount
    ) {
        Instant now = Instant.now();

        this.id = UUID.randomUUID();
        this.idempotencyKey = idempotencyKey;
        this.status = BulkOrderJobStatus.ACCEPTED;
        this.totalCount = totalCount;
        this.processedCount = 0;
        this.succeededCount = 0;
        this.failedCount = 0;
        this.createdAt = now;
        this.updatedAt = now;
        this.startedAt = now;
    }

    public void markProcessing() {
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }

        this.status = BulkOrderJobStatus.PROCESSING;
    }

    public void markItemSucceeded() {
        ensureProcessing();

        this.processedCount++;
        this.succeededCount++;

        completeIfFinished();
    }

    public void markItemFailed(String errorMessage) {
        ensureProcessing();

        this.processedCount++;
        this.failedCount++;

        if (errorMessage != null && !errorMessage.isBlank()) {
            if (this.errorMessage == null || this.errorMessage.isBlank()) {
                this.errorMessage = errorMessage;
            } else {
                this.errorMessage =
                        this.errorMessage + "; " + errorMessage;
            }
        }

        completeIfFinished();
    }

    public void markCompleted() {
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }

        this.status = BulkOrderJobStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void markCompletedWithErrors() {
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }

        this.status = BulkOrderJobStatus.COMPLETED_WITH_ERRORS;
        this.completedAt = Instant.now();
    }

    public void markFailed(String errorMessage) {
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }

        this.status = BulkOrderJobStatus.FAILED;
        this.completedAt = Instant.now();

        if (errorMessage != null && !errorMessage.isBlank()) {
            this.errorMessage = errorMessage;
        }
    }

    private void ensureProcessing() {
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }

        if (this.status == BulkOrderJobStatus.ACCEPTED) {
            this.status = BulkOrderJobStatus.PROCESSING;
        }
    }

    private void completeIfFinished() {
        if (this.processedCount < this.totalCount) {
            return;
        }

        this.completedAt = Instant.now();

        if (this.failedCount > 0) {
            this.status = BulkOrderJobStatus.COMPLETED_WITH_ERRORS;
        } else {
            this.status = BulkOrderJobStatus.COMPLETED;
        }
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
            status = BulkOrderJobStatus.ACCEPTED;
        }

        if (processedCount == null) {
            processedCount = 0;
        }

        if (succeededCount == null) {
            succeededCount = 0;
        }

        if (failedCount == null) {
            failedCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
