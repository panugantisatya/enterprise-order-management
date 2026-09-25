package com.enterprise.ordermanagement.order.service;

import com.enterprise.ordermanagement.order.bulk.BulkOrderJobCreatedEvent;
import com.enterprise.ordermanagement.order.dto.BulkCreateOrderRequest;
import com.enterprise.ordermanagement.order.dto.BulkOrderJobResponse;
import com.enterprise.ordermanagement.order.entity.BulkOrderJob;
import com.enterprise.ordermanagement.order.entity.OutboxEvent;
import com.enterprise.ordermanagement.order.repository.BulkOrderJobRepository;
import com.enterprise.ordermanagement.order.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Service
public class BulkOrderService {

    private static final String EVENT_TYPE = "BulkOrderJobCreated";
    private static final String AGGREGATE_TYPE = "BULK_ORDER_JOB";

    private final BulkOrderJobRepository bulkOrderJobRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public BulkOrderService(
            BulkOrderJobRepository bulkOrderJobRepository,
            OutboxEventRepository outboxEventRepository,
            JsonMapper jsonMapper
    ) {
        this.bulkOrderJobRepository = bulkOrderJobRepository;
        this.outboxEventRepository = outboxEventRepository;
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
                .orElseGet(
                        () -> createNewBulkJob(
                                idempotencyKey,
                                request
                        )
                );
    }

    @Transactional(readOnly = true)
    public BulkOrderJobResponse getBulkOrderJob(UUID jobId) {

        BulkOrderJob job =
                bulkOrderJobRepository.findById(jobId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Bulk order job not found: " + jobId
                                )
                        );

        return BulkOrderJobResponse.from(job);
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

        BulkOrderJobCreatedEvent event =
                new BulkOrderJobCreatedEvent(
                        UUID.randomUUID(),
                        EVENT_TYPE,
                        job.getId(),
                        Instant.now(),
                        request.orders()
                );

        try {
            String payload =
                    jsonMapper.writeValueAsString(event);

            OutboxEvent outboxEvent =
                    new OutboxEvent(
                            AGGREGATE_TYPE,
                            job.getId(),
                            EVENT_TYPE,
                            payload
                    );

            outboxEventRepository.save(outboxEvent);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to create bulk order outbox event",
                    ex
            );
        }

        return BulkOrderJobResponse.from(job);
    }
}
