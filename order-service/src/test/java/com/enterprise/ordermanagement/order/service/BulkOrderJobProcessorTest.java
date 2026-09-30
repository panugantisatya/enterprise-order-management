package com.enterprise.ordermanagement.order.service;

import com.enterprise.ordermanagement.order.bulk.BulkOrderJobItemService;
import com.enterprise.ordermanagement.order.bulk.BulkOrderJobProgressService;
import com.enterprise.ordermanagement.order.dto.BulkCreateOrderRequest;
import com.enterprise.ordermanagement.order.dto.CreateOrderItemRequest;
import com.enterprise.ordermanagement.order.dto.OrderResponse;
import com.enterprise.ordermanagement.order.entity.BulkOrderJobItem;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BulkOrderJobProcessorTest {

    @Test
    void shouldProcessAllItemsSuccessfully() {

        OrderService orderService =
                mock(OrderService.class);

        BulkOrderJobProgressService progressService =
                mock(BulkOrderJobProgressService.class);

        BulkOrderJobItemService itemService =
                mock(BulkOrderJobItemService.class);

        JsonMapper jsonMapper =
                new JsonMapper();

        UUID jobId = UUID.randomUUID();

        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();

        BulkOrderJobItem item1 =
                createItem(jobId, 0);

        BulkOrderJobItem item2 =
                createItem(jobId, 1);

        when(itemService.findItems(jobId))
                .thenReturn(List.of(item1, item2));

        when(itemService.claimItem(jobId, 0))
                .thenReturn(true);

        when(itemService.claimItem(jobId, 1))
                .thenReturn(true);

        OrderResponse response1 =
                mock(OrderResponse.class);

        OrderResponse response2 =
                mock(OrderResponse.class);

        when(response1.id())
                .thenReturn(orderId1);

        when(response2.id())
                .thenReturn(orderId2);

        when(orderService.createOrder(
                any(),
                anyString()
        )).thenReturn(
                response1,
                response2
        );

        BulkOrderJobProcessor processor =
                new BulkOrderJobProcessor(
                        orderService,
                        progressService,
                        itemService,
                        jsonMapper
                );

        processor.process(jobId);

        verify(progressService)
                .markProcessing(jobId);

        verify(itemService)
                .findItems(jobId);

        verify(itemService)
                .claimItem(jobId, 0);

        verify(itemService)
                .claimItem(jobId, 1);

        verify(orderService, times(2))
                .createOrder(
                        any(),
                        anyString()
                );

        verify(itemService)
                .markSucceeded(
                        jobId,
                        0,
                        orderId1
                );

        verify(itemService)
                .markSucceeded(
                        jobId,
                        1,
                        orderId2
                );

        verify(progressService, times(2))
                .markItemSucceeded(jobId);
    }

    @Test
    void shouldSkipItemsThatCannotBeClaimed() {

        OrderService orderService =
                mock(OrderService.class);

        BulkOrderJobProgressService progressService =
                mock(BulkOrderJobProgressService.class);

        BulkOrderJobItemService itemService =
                mock(BulkOrderJobItemService.class);

        JsonMapper jsonMapper =
                new JsonMapper();

        UUID jobId = UUID.randomUUID();

        BulkOrderJobItem item1 =
                createItem(jobId, 0);

        BulkOrderJobItem item2 =
                createItem(jobId, 1);

        when(itemService.findItems(jobId))
                .thenReturn(List.of(item1, item2));

        when(itemService.claimItem(
                jobId,
                0
        )).thenReturn(false);

        when(itemService.claimItem(
                jobId,
                1
        )).thenReturn(false);

        BulkOrderJobProcessor processor =
                new BulkOrderJobProcessor(
                        orderService,
                        progressService,
                        itemService,
                        jsonMapper
                );

        processor.process(jobId);

        verify(progressService)
                .markProcessing(jobId);

        verify(itemService)
                .findItems(jobId);

        verify(itemService)
                .claimItem(jobId, 0);

        verify(itemService)
                .claimItem(jobId, 1);

        verifyNoInteractions(orderService);

        verify(progressService, never())
                .markItemSucceeded(any());

        verify(progressService, never())
                .markItemFailed(any(), anyString());
    }

    @Test
    void shouldMarkItemFailedWhenOrderCreationFails() {

        OrderService orderService =
                mock(OrderService.class);

        BulkOrderJobProgressService progressService =
                mock(BulkOrderJobProgressService.class);

        BulkOrderJobItemService itemService =
                mock(BulkOrderJobItemService.class);

        JsonMapper jsonMapper =
                new JsonMapper();

        UUID jobId = UUID.randomUUID();

        BulkOrderJobItem item =
                createItem(jobId, 0);

        when(itemService.findItems(jobId))
                .thenReturn(List.of(item));

        when(itemService.claimItem(
                jobId,
                0
        )).thenReturn(true);

        when(orderService.createOrder(
                any(),
                anyString()
        )).thenThrow(
                new IllegalArgumentException(
                        "Invalid order"
                )
        );

        BulkOrderJobProcessor processor =
                new BulkOrderJobProcessor(
                        orderService,
                        progressService,
                        itemService,
                        jsonMapper
                );

        processor.process(jobId);

        verify(itemService)
                .markFailed(
                        eq(jobId),
                        eq(0),
                        contains("Invalid order")
                );

        verify(progressService)
                .markItemFailed(
                        eq(jobId),
                        contains("Invalid order")
                );

        verify(progressService, never())
                .markItemSucceeded(jobId);
    }

    private static BulkOrderJobItem createItem(
            UUID jobId,
            int index
    ) {

        BulkOrderJobItem item =
                new BulkOrderJobItem(
                        jobId,
                        index,
                        buildPayload()
                );

        /*
         * The entity starts in PROCESSING state.
         * For the processor unit test, we mock claimItem()
         * so the item can be treated as successfully claimed.
         */
        return item;
    }

    private static String buildPayload() {

        UUID customerId =
                UUID.randomUUID();

        UUID productId =
                UUID.randomUUID();

        BulkCreateOrderRequest.BulkOrderItem item =
                new BulkCreateOrderRequest.BulkOrderItem(
                        customerId,
                        "INR",
                        List.of(
                                new CreateOrderItemRequest(
                                        productId,
                                        2,
                                        new BigDecimal("100.00")
                                )
                        )
                );

        try {
            return new JsonMapper()
                    .writeValueAsString(item);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to create test payload",
                    ex
            );
        }
    }
}
