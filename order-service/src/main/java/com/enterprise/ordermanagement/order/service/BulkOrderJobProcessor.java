package com.enterprise.ordermanagement.order.service;

import com.enterprise.ordermanagement.order.bulk.BulkOrderJobItemService;
import com.enterprise.ordermanagement.order.bulk.BulkOrderJobProgressService;
import com.enterprise.ordermanagement.order.dto.BulkCreateOrderRequest;
import com.enterprise.ordermanagement.order.dto.CreateOrderRequest;
import com.enterprise.ordermanagement.order.entity.BulkOrderJobItem;
import com.enterprise.ordermanagement.order.exception.IdempotencyKeyConflictException;
import com.enterprise.ordermanagement.order.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Service
public class BulkOrderJobProcessor {

    private final OrderService orderService;
    private final BulkOrderJobProgressService progressService;
    private final BulkOrderJobItemService itemService;
    private final JsonMapper jsonMapper;

    public BulkOrderJobProcessor(
            OrderService orderService,
            BulkOrderJobProgressService progressService,
            BulkOrderJobItemService itemService,
            JsonMapper jsonMapper
    ) {
        this.orderService = orderService;
        this.progressService = progressService;
        this.itemService = itemService;
        this.jsonMapper = jsonMapper;
    }

    public void process(UUID jobId) {

        progressService.markProcessing(jobId);

        List<BulkOrderJobItem> items =
                itemService.findItems(jobId);

        for (BulkOrderJobItem item : items) {

            int index = item.getItemIndex();

            if (!itemService.claimItem(jobId, index)) {
                continue;
            }

            processItem(jobId, item);
        }
    }

    private void processItem(
            UUID jobId,
            BulkOrderJobItem item
    ) {

        int index = item.getItemIndex();

        try {

            BulkCreateOrderRequest.BulkOrderItem bulkItem =
                    jsonMapper.readValue(
                            item.getRequestPayload(),
                            BulkCreateOrderRequest.BulkOrderItem.class
                    );

            CreateOrderRequest orderRequest =
                    new CreateOrderRequest(
                            bulkItem.customerId(),
                            bulkItem.currency(),
                            bulkItem.items()
                    );

            String idempotencyKey =
                    "bulk:" + jobId + ":item:" + index;

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

        String message =
                exception.getMessage();

        if (message == null || message.isBlank()) {
            message =
                    exception
                            .getClass()
                            .getSimpleName();
        }

        return "Bulk item "
                + index
                + " failed: "
                + message;
    }
}
