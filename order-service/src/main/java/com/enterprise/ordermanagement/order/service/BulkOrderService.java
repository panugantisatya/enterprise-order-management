package com.enterprise.ordermanagement.order.service;

import com.enterprise.ordermanagement.order.bulk.BulkOrderJobCreatedEvent;
import com.enterprise.ordermanagement.order.bulk.BulkOrderJobItemService;
import com.enterprise.ordermanagement.order.dto.BulkCreateOrderRequest;
import com.enterprise.ordermanagement.order.dto.BulkOrderJobResponse;
import com.enterprise.ordermanagement.order.entity.BulkOrderJob;
import com.enterprise.ordermanagement.order.entity.OutboxEvent;
import com.enterprise.ordermanagement.order.exception.BulkOrderRetryNotAllowedException;
import com.enterprise.ordermanagement.order.repository.BulkOrderJobRepository;
import com.enterprise.ordermanagement.order.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Service
public class BulkOrderService {

    private static final String EVENT_TYPE =
            "BulkOrderJobCreated";

    private static final String AGGREGATE_TYPE =
            "BULK_ORDER_JOB";

    private final BulkOrderJobRepository bulkOrderJobRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final BulkOrderJobItemService itemService;
    private final JsonMapper jsonMapper;

    public BulkOrderService(
            BulkOrderJobRepository bulkOrderJobRepository,
            OutboxEventRepository outboxEventRepository,
            BulkOrderJobItemService itemService,
            JsonMapper jsonMapper
    ) {
        this.bulkOrderJobRepository = bulkOrderJobRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.itemService = itemService;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public BulkOrderJobResponse createBulkOrder(
            String idempotencyKey,
            BulkCreateOrderRequest request
    ) {

        return bulkOrderJobRepository
                .findByIdempotencyKey(idempotencyKey)
                .map(BulkOrderJobResponse::from)
                .orElseGet(() ->
                        createNewBulkJob(
                                idempotencyKey,
                                request
                        )
                );
    }

    @Transactional(readOnly = true)
    public BulkOrderJobResponse getBulkOrderJob(
            UUID jobId
    ) {
        return bulkOrderJobRepository
                .findById(jobId)
                .map(BulkOrderJobResponse::from)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Bulk order job not found: " + jobId
                        )
                );
    }

    @Transactional
    public BulkOrderJobResponse retryFailedItems(
            UUID jobId
    ) {

        int reopened =
                bulkOrderJobRepository
                        .reopenFailedItemsForRetry(jobId);

        if (reopened == 0) {
            throw new BulkOrderRetryNotAllowedException(
                    "Bulk order job "
                            + jobId
                            + " is not in COMPLETED_WITH_ERRORS "
                            + "with failed items available for retry"
            );
        }

        int requeued =
                itemService.requeueFailedItems(jobId);

        if (requeued != reopened) {
            throw new IllegalStateException(
                    "Bulk order retry state mismatch for job "
                            + jobId
                            + ": expected "
                            + reopened
                            + " failed items but requeued "
                            + requeued
            );
        }

        BulkOrderJobCreatedEvent event =
                new BulkOrderJobCreatedEvent(
                        UUID.randomUUID(),
                        EVENT_TYPE,
                        jobId,
                        Instant.now()
                );

        String payload;

        try {
            payload =
                    jsonMapper.writeValueAsString(event);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to serialize bulk order retry event",
                    ex
            );
        }

        OutboxEvent outbox =
                new OutboxEvent(
                        AGGREGATE_TYPE,
                        jobId,
                        EVENT_TYPE,
                        payload
                );

        outboxEventRepository.save(outbox);

        return bulkOrderJobRepository
                .findById(jobId)
                .map(BulkOrderJobResponse::from)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Bulk order job disappeared during retry: "
                                        + jobId
                        )
                );
    }

    private BulkOrderJobResponse createNewBulkJob(
            String idempotencyKey,
            BulkCreateOrderRequest request
    ) {

        BulkOrderJob job =
                new BulkOrderJob(
                        idempotencyKey,
                        request.orders().size()
                );

        bulkOrderJobRepository.save(job);

        itemService.createItems(
                job.getId(),
                request.orders()
        );

        BulkOrderJobCreatedEvent event =
                new BulkOrderJobCreatedEvent(
                        UUID.randomUUID(),
                        EVENT_TYPE,
                        job.getId(),
                        Instant.now()
                );

        String payload;

        try {
            payload =
                    jsonMapper.writeValueAsString(event);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to serialize bulk order event",
                    ex
            );
        }

        OutboxEvent outbox =
                new OutboxEvent(
                        AGGREGATE_TYPE,
                        job.getId(),
                        EVENT_TYPE,
                        payload
                );

        outboxEventRepository.save(outbox);

        return BulkOrderJobResponse.from(job);
    }
}
