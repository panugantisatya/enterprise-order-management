package com.enterprise.ordermanagement.order.dto;

import com.enterprise.ordermanagement.order.entity.BulkOrderJob;

import java.time.Instant;
import java.util.UUID;

public record BulkOrderJobResponse(
        UUID jobId,
        String status,
        int totalCount,
        int processedCount,
        int succeededCount,
        int failedCount,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        String errorMessage
) {

    public static BulkOrderJobResponse from(BulkOrderJob job) {
        return new BulkOrderJobResponse(
                job.getId(),
                job.getStatus().name(),
                job.getTotalCount(),
                job.getProcessedCount(),
                job.getSucceededCount(),
                job.getFailedCount(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getErrorMessage()
        );
    }
}
