package com.enterprise.ordermanagement.order.service;

import com.enterprise.ordermanagement.order.bulk.BulkOrderJobProgressService;
import com.enterprise.ordermanagement.order.dto.BulkCreateOrderRequest;
import com.enterprise.ordermanagement.order.dto.CreateOrderItemRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class BulkOrderJobProcessorTest {

    @Test
    void shouldProcessEveryBulkItem() {

        OrderService orderService =
                mock(OrderService.class);

        BulkOrderJobProgressService progressService =
                mock(BulkOrderJobProgressService.class);

        BulkOrderJobProcessor processor =
                new BulkOrderJobProcessor(
                        orderService,
                        progressService
                );

        UUID jobId = UUID.randomUUID();

        List<BulkCreateOrderRequest.BulkOrderItem> items =
                List.of(
                        createItem(),
                        createItem(),
                        createItem()
                );

        processor.process(jobId, items);

        verify(progressService).markProcessing(jobId);

        verify(
                orderService,
                times(3)
        ).createOrder(
                any(),
                anyString()
        );

        verify(
                progressService,
                times(3)
        ).markItemSucceeded(jobId);
    }

    @Test
    void shouldContinueAfterIndividualItemFailure() {

        OrderService orderService =
                mock(OrderService.class);

        BulkOrderJobProgressService progressService =
                mock(BulkOrderJobProgressService.class);

        BulkOrderJobProcessor processor =
                new BulkOrderJobProcessor(
                        orderService,
                        progressService
                );

        UUID jobId = UUID.randomUUID();

        List<BulkCreateOrderRequest.BulkOrderItem> items =
                List.of(
                        createItem(),
                        createItem(),
                        createItem()
                );

        doAnswer(invocation -> {

            String idempotencyKey =
                    invocation.getArgument(1);

            if (idempotencyKey.endsWith(":item:1")) {
                throw new IllegalArgumentException(
                        "Invalid order item"
                );
            }

            return null;

        }).when(orderService)
                .createOrder(
                        any(),
                        anyString()
                );

        processor.process(jobId, items);

        verify(
                orderService,
                times(3)
        ).createOrder(
                any(),
                anyString()
        );

        verify(
                progressService,
                times(2)
        ).markItemSucceeded(jobId);

        verify(
                progressService,
                times(1)
        ).markItemFailed(
                any(),
                anyString()
        );
    }

    private BulkCreateOrderRequest.BulkOrderItem createItem() {

        return new BulkCreateOrderRequest.BulkOrderItem(
                UUID.randomUUID(),
                "INR",
                List.of(
                        new CreateOrderItemRequest(
                                UUID.randomUUID(),
                                2,
                                new BigDecimal("100.00")
                        )
                )
        );
    }
}
