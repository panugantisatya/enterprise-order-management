package com.enterprise.ordermanagement.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "bulk_order_job_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_bulk_order_job_items_job_index",
                        columnNames = {"job_id", "item_index"}
                )
        }
)
@Getter
@NoArgsConstructor
public class BulkOrderJobItem {

    public enum BulkOrderJobItemStatus {
        PROCESSING,
        SUCCEEDED,
        FAILED
    }

    @Id
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "item_index", nullable = false)
    private Integer itemIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BulkOrderJobItemStatus status;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public BulkOrderJobItem(UUID jobId, int itemIndex) {
        this.id = UUID.randomUUID();
        this.jobId = jobId;
        this.itemIndex = itemIndex;
        this.status = BulkOrderJobItemStatus.PROCESSING;
        this.processingStartedAt = Instant.now();
    }

    public boolean isProcessingStale(Instant cutoff) {
        return status == BulkOrderJobItemStatus.PROCESSING
                && processingStartedAt != null
                && processingStartedAt.isBefore(cutoff);
    }

    public void reclaim() {
        this.status = BulkOrderJobItemStatus.PROCESSING;
        this.processingStartedAt = Instant.now();
        this.errorMessage = null;
        this.completedAt = null;
    }

    public void markSucceeded(UUID orderId) {
        this.status = BulkOrderJobItemStatus.SUCCEEDED;
        this.orderId = orderId;
        this.errorMessage = null;
        this.completedAt = Instant.now();
    }

    public void markFailed(String errorMessage) {
        this.status = BulkOrderJobItemStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
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

        if (status == BulkOrderJobItemStatus.PROCESSING
                && processingStartedAt == null) {
            processingStartedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
