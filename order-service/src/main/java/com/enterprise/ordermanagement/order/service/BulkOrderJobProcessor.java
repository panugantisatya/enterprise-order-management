package com.enterprise.ordermanagement.order.service;

import com.enterprise.ordermanagement.order.bulk.BulkOrderJobProgressService;
import com.enterprise.ordermanagement.order.dto.BulkCreateOrderRequest;
import com.enterprise.ordermanagement.order.dto.CreateOrderRequest;
import com.enterprise.ordermanagement.order.exception.IdempotencyKeyConflictException;
import com.enterprise.ordermanagement.order.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BulkOrderJobProcessor {

    private final OrderService orderService;
    private final BulkOrderJobProgressService progressService;

    public BulkOrderJobProcessor(
            OrderService orderService,
            BulkOrderJobProgressService progressService
    ) {
        this.orderService = orderService;
        this.progressService = progressService;
    }

    /*
     * Deliberately NOT transactional.
     *
     * Each OrderService.createOrder() call owns its own transaction.
     * This ensures one failed bulk item does not roll back successful
     * items that were already processed.
     */
    public void process(
            UUID jobId,
            List<BulkCreateOrderRequest.BulkOrderItem> items
    ) {
        progressService.markProcessing(jobId);

        for (int index = 0; index < items.size(); index++) {
            BulkCreateOrderRequest.BulkOrderItem item = items.get(index);

            processItem(
                    jobId,
                    index,
                    item
            );
        }
    }

    private void processItem(
            UUID jobId,
            int index,
            BulkCreateOrderRequest.BulkOrderItem item
    ) {
        /*
         * Deterministic idempotency key.
         *
         * If Kafka redelivers the same bulk job, the same item gets
         * the same key and OrderService can safely return the existing
         * order instead of creating a duplicate.
         */
        String idempotencyKey =
                "bulk:" + jobId + ":item:" + index;

        try {
            CreateOrderRequest orderRequest =
                    new CreateOrderRequest(
                            item.customerId(),
                            item.currency(),
                            item.items()
                    );

            /*
             * OrderService owns the database transaction for this item.
             */
            orderService.createOrder(
                    orderRequest,
                    idempotencyKey
            );

            /*
             * Progress is committed independently.
             */
            progressService.markItemSucceeded(jobId);

        } catch (
                IdempotencyKeyConflictException
                | ResourceNotFoundException
                | IllegalArgumentException ex
        ) {

            /*
             * Business/item failure:
             * record the failure and continue with the next item.
             */
            progressService.markItemFailed(
                    jobId,
                    buildItemError(index, ex)
            );
        }
    }

    private String buildItemError(
            int index,
            Exception exception
    ) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }

        return "Item " + index + " failed: " + message;
    }
}
