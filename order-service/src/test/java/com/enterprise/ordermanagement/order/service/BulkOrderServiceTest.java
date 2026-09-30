package com.enterprise.ordermanagement.order.service;

import com.enterprise.ordermanagement.order.bulk.BulkOrderJobItemService;
import com.enterprise.ordermanagement.order.dto.BulkOrderJobResponse;
import com.enterprise.ordermanagement.order.entity.BulkOrderJob;
import com.enterprise.ordermanagement.order.entity.OutboxEvent;
import com.enterprise.ordermanagement.order.exception.BulkOrderRetryLimitExceededException;
import com.enterprise.ordermanagement.order.exception.BulkOrderRetryNotAllowedException;
import com.enterprise.ordermanagement.order.repository.BulkOrderJobRepository;
import com.enterprise.ordermanagement.order.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BulkOrderServiceTest {

    private BulkOrderJobRepository bulkOrderJobRepository;
    private OutboxEventRepository outboxEventRepository;
    private BulkOrderJobItemService itemService;
    private JsonMapper jsonMapper;

    private BulkOrderService service;

    @BeforeEach
    void setUp() {

        bulkOrderJobRepository =
                mock(BulkOrderJobRepository.class);

        outboxEventRepository =
                mock(OutboxEventRepository.class);

        itemService =
                mock(BulkOrderJobItemService.class);

        jsonMapper =
                new JsonMapper();

        service =
                new BulkOrderService(
                        bulkOrderJobRepository,
                        outboxEventRepository,
                        itemService,
                        jsonMapper,
                        3
                );
    }

    @Test
    void shouldRetryFailedItemsAndIncrementRetryCount() {

        UUID jobId =
                UUID.randomUUID();

        BulkOrderJob job =
                new BulkOrderJob(
                        "retry-test-001",
                        3
                );

        job.markProcessing();
        job.markItemSucceeded();
        job.markItemSucceeded();
        job.markItemFailed("temporary failure");

        assertEquals(
                BulkOrderJob.BulkOrderJobStatus.COMPLETED_WITH_ERRORS,
                job.getStatus()
        );

        assertEquals(
                0,
                job.getRetryCount()
        );

        when(
                bulkOrderJobRepository
                        .reopenFailedItemsForRetry(jobId, 3)
        ).thenReturn(1);

        when(
                itemService.requeueFailedItems(jobId)
        ).thenReturn(1);

        when(
                bulkOrderJobRepository.findById(jobId)
        ).thenReturn(Optional.of(job));

        BulkOrderJobResponse response =
                service.retryFailedItems(jobId);

        assertNotNull(response);

        assertEquals(
                0,
                response.retryCount()
        );

        verify(
                bulkOrderJobRepository
        ).reopenFailedItemsForRetry(jobId, 3);

        verify(
                itemService
        ).requeueFailedItems(jobId);

        verify(
                outboxEventRepository
        ).save(
                argThat(event ->
                        event.getAggregateId()
                                .equals(jobId)
                                && event.getEventType()
                                .equals("BulkOrderJobCreated")
                )
        );
    }

    @Test
    void shouldRejectRetryWhenJobIsNotRetryable() {

        UUID jobId =
                UUID.randomUUID();

        when(
                bulkOrderJobRepository
                        .reopenFailedItemsForRetry(jobId, 3)
        ).thenReturn(0);

        BulkOrderJob job =
                new BulkOrderJob(
                        "not-retryable",
                        2
                );

        when(
                bulkOrderJobRepository.findById(jobId)
        ).thenReturn(Optional.of(job));

        assertThrows(
                BulkOrderRetryNotAllowedException.class,
                () ->
                        service.retryFailedItems(jobId)
        );

        verify(
                itemService,
                never()
        ).requeueFailedItems(any());

        verify(
                outboxEventRepository,
                never()
        ).save(any(OutboxEvent.class));
    }

    @Test
    void shouldRejectRetryWhenRetryLimitIsExceeded() {

        UUID jobId =
                UUID.randomUUID();

        when(
                bulkOrderJobRepository
                        .reopenFailedItemsForRetry(jobId, 3)
        ).thenReturn(0);

        BulkOrderJob job =
                new BulkOrderJob(
                        "retry-limit",
                        2
                );

        job.markProcessing();
        job.markItemFailed("failure-1");
        job.markItemFailed("failure-2");

        assertEquals(
                BulkOrderJob.BulkOrderJobStatus.COMPLETED_WITH_ERRORS,
                job.getStatus()
        );

        /*
         * retryCount is database-managed in M15.
         * This test uses a repository spy to represent the
         * persisted state after three successful retry attempts.
         */
        BulkOrderJob persistedJob =
                spy(job);

        doReturn(3)
                .when(persistedJob)
                .getRetryCount();

        when(
                bulkOrderJobRepository.findById(jobId)
        ).thenReturn(Optional.of(persistedJob));

        BulkOrderRetryLimitExceededException exception =
                assertThrows(
                        BulkOrderRetryLimitExceededException.class,
                        () ->
                                service.retryFailedItems(jobId)
                );

        assertTrue(
                exception.getMessage()
                        .contains("maximum retry limit of 3")
        );

        verify(
                itemService,
                never()
        ).requeueFailedItems(any());

        verify(
                outboxEventRepository,
                never()
        ).save(any(OutboxEvent.class));
    }

    @Test
    void shouldRejectConcurrentRetryAfterAnotherRetryClaimsJob() {

        UUID jobId =
                UUID.randomUUID();

        when(
                bulkOrderJobRepository
                        .reopenFailedItemsForRetry(jobId, 3)
        ).thenReturn(0);

        BulkOrderJob job =
                new BulkOrderJob(
                        "concurrent-retry",
                        2
                );

        job.markProcessing();
        job.markItemSucceeded();
        job.markItemFailed("temporary");

        /*
         * The atomic UPDATE returned zero because another request
         * already changed the job from COMPLETED_WITH_ERRORS to
         * PROCESSING.
         */
        job.markProcessing();

        when(
                bulkOrderJobRepository.findById(jobId)
        ).thenReturn(Optional.of(job));

        assertThrows(
                BulkOrderRetryNotAllowedException.class,
                () ->
                        service.retryFailedItems(jobId)
        );

        verify(
                itemService,
                never()
        ).requeueFailedItems(any());

        verify(
                outboxEventRepository,
                never()
        ).save(any(OutboxEvent.class));
    }

    @Test
    void shouldRollbackRetryWhenItemCountDoesNotMatch() {

        UUID jobId =
                UUID.randomUUID();

        when(
                bulkOrderJobRepository
                        .reopenFailedItemsForRetry(jobId, 3)
        ).thenReturn(2);

        when(
                itemService.requeueFailedItems(jobId)
        ).thenReturn(1);

        assertThrows(
                IllegalStateException.class,
                () ->
                        service.retryFailedItems(jobId)
        );

        verify(
                outboxEventRepository,
                never()
        ).save(any(OutboxEvent.class));
    }
}
