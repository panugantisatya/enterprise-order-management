package com.enterprise.ordermanagement.order.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BulkOrderJobTest {

    @Test
    void newJobShouldStartAsAccepted() {
        BulkOrderJob job =
                new BulkOrderJob(
                        "bulk-test-001",
                        3
                );

        assertEquals(
                BulkOrderJob.BulkOrderJobStatus.ACCEPTED,
                job.getStatus()
        );

        assertEquals(3, job.getTotalCount());
        assertEquals(0, job.getProcessedCount());
        assertEquals(0, job.getSucceededCount());
        assertEquals(0, job.getFailedCount());
        assertNotNull(job.getId());
        assertNotNull(job.getCreatedAt());
    }

    @Test
    void jobShouldBecomeProcessing() {
        BulkOrderJob job =
                new BulkOrderJob(
                        "bulk-test-002",
                        2
                );

        job.markProcessing();

        assertEquals(
                BulkOrderJob.BulkOrderJobStatus.PROCESSING,
                job.getStatus()
        );

        assertNotNull(job.getStartedAt());
    }

    @Test
    void allSuccessfulItemsShouldCompleteJob() {
        BulkOrderJob job =
                new BulkOrderJob(
                        "bulk-test-003",
                        2
                );

        job.markProcessing();
        job.markItemSucceeded();
        job.markItemSucceeded();

        assertEquals(
                BulkOrderJob.BulkOrderJobStatus.COMPLETED,
                job.getStatus()
        );

        assertEquals(2, job.getProcessedCount());
        assertEquals(2, job.getSucceededCount());
        assertEquals(0, job.getFailedCount());
        assertNotNull(job.getCompletedAt());
    }

    @Test
    void failedItemShouldCompleteJobWithErrors() {
        BulkOrderJob job =
                new BulkOrderJob(
                        "bulk-test-004",
                        2
                );

        job.markProcessing();

        job.markItemSucceeded();
        job.markItemFailed("Invalid customer");

        assertEquals(
                BulkOrderJob.BulkOrderJobStatus.COMPLETED_WITH_ERRORS,
                job.getStatus()
        );

        assertEquals(2, job.getProcessedCount());
        assertEquals(1, job.getSucceededCount());
        assertEquals(1, job.getFailedCount());

        assertEquals(
                "Invalid customer",
                job.getErrorMessage()
        );
    }

    @Test
    void failedJobShouldStoreError() {
        BulkOrderJob job =
                new BulkOrderJob(
                        "bulk-test-005",
                        10
                );

        job.markFailed("Kafka unavailable");

        assertEquals(
                BulkOrderJob.BulkOrderJobStatus.FAILED,
                job.getStatus()
        );

        assertEquals(
                "Kafka unavailable",
                job.getErrorMessage()
        );

        assertNotNull(job.getCompletedAt());
    }
}
