package com.enterprise.ordermanagement.order.service;

import com.enterprise.ordermanagement.order.bulk.BulkOrderJobItemService;
import com.enterprise.ordermanagement.order.bulk.BulkOrderJobProgressService;
import com.enterprise.ordermanagement.order.dto.BulkCreateOrderRequest;
import com.enterprise.ordermanagement.order.dto.CreateOrderRequest;
import com.enterprise.ordermanagement.order.entity.BulkOrderJobItem;
import com.enterprise.ordermanagement.order.exception.IdempotencyKeyConflictException;
import com.enterprise.ordermanagement.order.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
public class BulkOrderJobProcessor {

    private final OrderService orderService;
    private final BulkOrderJobProgressService progressService;
    private final BulkOrderJobItemService itemService;
    private final ThreadPoolTaskExecutor bulkTaskExecutor;
    private final JsonMapper jsonMapper;
    private final int maxInFlight;

    public BulkOrderJobProcessor(
            OrderService orderService,
            BulkOrderJobProgressService progressService,
            BulkOrderJobItemService itemService,
            @Qualifier("bulkTaskExecutor")
            ThreadPoolTaskExecutor bulkTaskExecutor,
            JsonMapper jsonMapper,
            @Value("${bulk.processing.max-in-flight:8}")
            int maxInFlight
    ) {

        if (maxInFlight <= 0) {
            throw new IllegalArgumentException(
                    "bulk.processing.max-in-flight must be greater than zero"
            );
        }

        this.orderService = orderService;
        this.progressService = progressService;
        this.itemService = itemService;
        this.bulkTaskExecutor = bulkTaskExecutor;
        this.jsonMapper = jsonMapper;
        this.maxInFlight = maxInFlight;
    }

    /*
     * Deliberately NOT transactional.
     *
     * 12F durable payload is the source of truth.
     * Kafka provides the jobId only.
     *
     * Milestone 13 adds bounded concurrency while
     * preserving the existing item-level idempotency
     * and recovery boundary.
     */
    public void process(UUID jobId) {

        progressService.markProcessing(jobId);

        List<BulkOrderJobItem> items =
                itemService.findItems(jobId);

        List<CompletableFuture<Void>> inFlight =
                new ArrayList<>(maxInFlight);

        for (BulkOrderJobItem item : items) {

            if (inFlight.size() >= maxInFlight) {
                waitForOne(inFlight);
            }

            CompletableFuture<Void> future =
                    CompletableFuture.runAsync(
                            () -> processItem(
                                    jobId,
                                    item
                            ),
                            bulkTaskExecutor
                    );

            inFlight.add(future);
        }

        waitForAll(inFlight);
    }

    private void processItem(
            UUID jobId,
            BulkOrderJobItem item
    ) {

        int index = item.getItemIndex();

        /*
         * Existing 12F idempotency/recovery boundary.
         */
        if (!itemService.claimItem(jobId, index)) {
            return;
        }

        try {

            BulkCreateOrderRequest.BulkOrderItem bulkItem =
                    jsonMapper.readValue(
                            item.getRequestPayload(),
                            BulkCreateOrderRequest.BulkOrderItem.class
                    );

            String idempotencyKey =
                    "bulk:" + jobId + ":item:" + index;

            CreateOrderRequest orderRequest =
                    new CreateOrderRequest(
                            bulkItem.customerId(),
                            bulkItem.currency(),
                            bulkItem.items()
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

        } catch (JacksonException ex) {

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

    private void waitForOne(
            List<CompletableFuture<Void>> inFlight
    ) {

        CompletableFuture<Void> first =
                inFlight.remove(0);

        waitFor(first);
    }

    private void waitForAll(
            List<CompletableFuture<Void>> inFlight
    ) {

        while (!inFlight.isEmpty()) {
            waitForOne(inFlight);
        }
    }

    private void waitFor(
            CompletableFuture<Void> future
    ) {

        try {

            future.join();

        } catch (CompletionException ex) {

            Throwable cause =
                    ex.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw ex;
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
                    exception.getClass()
                            .getSimpleName();
        }

        return "Bulk item "
                + index
                + " failed: "
                + message;
    }
}
