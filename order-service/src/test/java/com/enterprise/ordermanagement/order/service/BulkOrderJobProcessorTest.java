package com.enterprise.ordermanagement.order.service;

import com.enterprise.ordermanagement.order.bulk.BulkOrderJobItemService;
import com.enterprise.ordermanagement.order.bulk.BulkOrderJobProgressService;
import com.enterprise.ordermanagement.order.dto.BulkCreateOrderRequest;
import com.enterprise.ordermanagement.order.dto.CreateOrderItemRequest;
import com.enterprise.ordermanagement.order.dto.CreateOrderRequest;
import com.enterprise.ordermanagement.order.dto.OrderResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

        UUID jobId = UUID.randomUUID();

        when(itemService.claimItem(
                eq(jobId),
                anyInt()
        )).thenReturn(true);

        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();

        OrderResponse response1 = mock(OrderResponse.class);
        OrderResponse response2 = mock(OrderResponse.class);

        when(response1.id()).thenReturn(orderId1);
        when(response2.id()).thenReturn(orderId2);

        when(orderService.createOrder(
                any(CreateOrderRequest.class),
                anyString()
        )).thenReturn(response1, response2);

        BulkOrderJobProcessor processor =
                new BulkOrderJobProcessor(
                        orderService,
                        progressService,
                        itemService
                );

        List<BulkCreateOrderRequest.BulkOrderItem> items =
                List.of(
                        createItem(),
                        createItem()
                );

        processor.process(jobId, items);

        verify(progressService).markProcessing(jobId);

        verify(itemService).claimItem(jobId, 0);
        verify(itemService).claimItem(jobId, 1);

        verify(orderService, times(2))
                .createOrder(
                        any(CreateOrderRequest.class),
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
    void shouldSkipAlreadyClaimedItems() {

        OrderService orderService =
                mock(OrderService.class);

        BulkOrderJobProgressService progressService =
                mock(BulkOrderJobProgressService.class);

        BulkOrderJobItemService itemService =
                mock(BulkOrderJobItemService.class);

        UUID jobId = UUID.randomUUID();

        when(itemService.claimItem(
                eq(jobId),
                anyInt()
        )).thenReturn(false);

        BulkOrderJobProcessor processor =
                new BulkOrderJobProcessor(
                        orderService,
                        progressService,
                        itemService
                );

        List<BulkCreateOrderRequest.BulkOrderItem> items =
                List.of(
                        createItem(),
                        createItem()
                );

        processor.process(jobId, items);

        verify(progressService).markProcessing(jobId);

        verify(itemService).claimItem(jobId, 0);
        verify(itemService).claimItem(jobId, 1);

        verifyNoInteractions(orderService);

        verify(itemService, never())
                .markSucceeded(
                        any(UUID.class),
                        anyInt(),
                        any(UUID.class)
                );

        verify(itemService, never())
                .markFailed(
                        any(UUID.class),
                        anyInt(),
                        anyString()
                );

        verify(progressService, never())
                .markItemSucceeded(any(UUID.class));

        verify(progressService, never())
                .markItemFailed(
                        any(UUID.class),
                        anyString()
                );
    }

    @Test
    void shouldMarkItemFailedWhenOrderCreationFails() {

        OrderService orderService =
                mock(OrderService.class);

        BulkOrderJobProgressService progressService =
                mock(BulkOrderJobProgressService.class);

        BulkOrderJobItemService itemService =
                mock(BulkOrderJobItemService.class);

        UUID jobId = UUID.randomUUID();

        when(itemService.claimItem(
                eq(jobId),
                anyInt()
        )).thenReturn(true);

        when(orderService.createOrder(
                any(CreateOrderRequest.class),
                anyString()
        )).thenThrow(
                new IllegalArgumentException("Invalid order")
        );

        BulkOrderJobProcessor processor =
                new BulkOrderJobProcessor(
                        orderService,
                        progressService,
                        itemService
                );

        processor.process(
                jobId,
                List.of(createItem())
        );

        verify(itemService).claimItem(jobId, 0);

        verify(itemService).markFailed(
                eq(jobId),
                eq(0),
                contains("Invalid order")
        );

        verify(progressService).markItemFailed(
                eq(jobId),
                contains("Invalid order")
        );

        verify(progressService, never())
                .markItemSucceeded(jobId);
    }

    private static BulkCreateOrderRequest.BulkOrderItem createItem() {

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
