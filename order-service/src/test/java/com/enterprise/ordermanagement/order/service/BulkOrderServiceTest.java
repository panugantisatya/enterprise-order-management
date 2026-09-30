package com.enterprise.ordermanagement.order.service;

import com.enterprise.ordermanagement.order.bulk.BulkOrderJobItemService;
import com.enterprise.ordermanagement.order.dto.BulkOrderJobResponse;
import com.enterprise.ordermanagement.order.entity.BulkOrderJob;
import com.enterprise.ordermanagement.order.entity.OutboxEvent;
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
                        jsonMapper
                );
    }

    @Test
    void shouldRetryFailedItemsAndCreateNewOutboxEvent() {

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

        when(
                bulkOrderJobRepository
                        .reopenFailedItemsForRetry(jobId)
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

        verify(
                bulkOrderJobRepository
        ).reopenFailedItemsForRetry(jobId);

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
                        .reopenFailedItemsForRetry(jobId)
        ).thenReturn(0);

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
                        .reopenFailedItemsForRetry(jobId)
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
