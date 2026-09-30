package com.enterprise.ordermanagement.order.service;

import com.enterprise.ordermanagement.order.bulk.BulkOrderJobItemService;
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
    private final BulkOrderJobItemService itemService;

    public BulkOrderJobProcessor(
            OrderService orderService,
            BulkOrderJobProgressService progressService,
            BulkOrderJobItemService itemService
    ) {
        this.orderService = orderService;
        this.progressService = progressService;
        this.itemService = itemService;
    }

    /*
     * Deliberately NOT transactional.
     *
     * Each order owns its own transaction.
     * Each bulk-item state transition also owns
     * its own short transaction.
     */
    public void process(
            UUID jobId,
            List<BulkCreateOrderRequest.BulkOrderItem> items
    ) {

        progressService.markProcessing(jobId);

        for (int index = 0; index < items.size(); index++) {

            if (!itemService.claimItem(jobId, index)) {
                /*
                 * This item was already claimed/completed by
                 * an earlier delivery or another consumer.
                 *
                 * Never increment job counters again.
                 */
                continue;
            }

            processItem(
                    jobId,
                    index,
                    items.get(index)
            );
        }
    }

    private void processItem(
            UUID jobId,
            int index,
            BulkCreateOrderRequest.BulkOrderItem item
    ) {

        String idempotencyKey =
                "bulk:" + jobId + ":item:" + index;

        try {

            CreateOrderRequest orderRequest =
                    new CreateOrderRequest(
                            item.customerId(),
                            item.currency(),
                            item.items()
                    );

            var order =
                    orderService.createOrder(
                            orderRequest,
                            idempotencyKey
                    );

            itemService.markSucceeded(
                    jobId,
                    index,
                    order.id()
            );

            progressService.markItemSucceeded(jobId);

        } catch (
                IdempotencyKeyConflictException
                | ResourceNotFoundException
                | IllegalArgumentException ex
        ) {

            String error =
                    buildItemError(index, ex);

            itemService.markFailed(
                    jobId,
                    index,
                    error
            );

            progressService.markItemFailed(
                    jobId,
                    error
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

        return "Bulk item " + index + " failed: " + message;
    }
}
