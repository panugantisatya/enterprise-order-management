package com.enterprise.ordermanagement.order.service;

import com.enterprise.ordermanagement.order.bulk.BulkOrderJobItemService;
import com.enterprise.ordermanagement.order.bulk.BulkOrderJobProgressService;
import com.enterprise.ordermanagement.order.dto.BulkCreateOrderRequest;
import com.enterprise.ordermanagement.order.dto.CreateOrderItemRequest;
import com.enterprise.ordermanagement.order.dto.CreateOrderRequest;
import com.enterprise.ordermanagement.order.dto.OrderResponse;
import com.enterprise.ordermanagement.order.entity.BulkOrderJobItem;
import com.enterprise.ordermanagement.order.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BulkOrderJobProcessorTest {

    private ThreadPoolTaskExecutor executor;

    private OrderService orderService;
    private BulkOrderJobProgressService progressService;
    private BulkOrderJobItemService itemService;

    private JsonMapper jsonMapper;

    private BulkOrderJobProcessor processor;

    @BeforeEach
    void setUp() {

        executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(2);
        executor.setThreadNamePrefix("test-bulk-");
        executor.initialize();

        orderService =
                mock(OrderService.class);

        progressService =
                mock(BulkOrderJobProgressService.class);

        itemService =
                mock(BulkOrderJobItemService.class);

        jsonMapper =
                new JsonMapper();

        processor =
                new BulkOrderJobProcessor(
                        orderService,
                        progressService,
                        itemService,
                        executor,
                        jsonMapper,
                        4
                );
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void shouldProcessBulkItemsWithBoundedConcurrency()
            throws Exception {

        UUID jobId =
                UUID.randomUUID();

        List<BulkOrderJobItem> items =
                persistedItems(
                        jobId,
                        6
                );

        when(itemService.findItems(jobId))
                .thenReturn(items);

        when(itemService.claimItem(
                eq(jobId),
                anyInt()
        )).thenReturn(true);

        AtomicInteger active =
                new AtomicInteger();

        AtomicInteger maxActive =
                new AtomicInteger();

        when(orderService.createOrder(
                any(CreateOrderRequest.class),
                anyString()
        )).thenAnswer(invocation -> {

            int current =
                    active.incrementAndGet();

            maxActive.updateAndGet(
                    previous ->
                            Math.max(previous, current)
            );

            try {
                Thread.sleep(50);
            } finally {
                active.decrementAndGet();
            }

            return mock(OrderResponse.class);
        });

        processor.process(jobId);

        verify(
                itemService
        ).findItems(jobId);

        verify(
                orderService,
                times(6)
        ).createOrder(
                any(CreateOrderRequest.class),
                anyString()
        );

        /*
         * The executor has only two worker threads.
         */
        assertTrue(
                maxActive.get() <= 2,
                "Concurrent processing exceeded worker count"
        );

        verify(
                progressService
        ).markProcessing(jobId);

        verify(
                progressService,
                times(6)
        ).markItemSucceeded(jobId);
    }

    @Test
    void shouldNotProcessAlreadyClaimedItems() {

        UUID jobId =
                UUID.randomUUID();

        List<BulkOrderJobItem> items =
                persistedItems(
                        jobId,
                        3
                );

        when(itemService.findItems(jobId))
                .thenReturn(items);

        when(itemService.claimItem(
                eq(jobId),
                eq(0)
        )).thenReturn(false);

        when(itemService.claimItem(
                eq(jobId),
                eq(1)
        )).thenReturn(true);

        when(itemService.claimItem(
                eq(jobId),
                eq(2)
        )).thenReturn(false);

        when(orderService.createOrder(
                any(CreateOrderRequest.class),
                anyString()
        )).thenReturn(
                mock(OrderResponse.class)
        );

        processor.process(jobId);

        verify(
                itemService
        ).findItems(jobId);

        verify(
                orderService,
                times(1)
        ).createOrder(
                any(CreateOrderRequest.class),
                anyString()
        );
    }

    @Test
    void shouldMarkExpectedItemFailure() {

        UUID jobId =
                UUID.randomUUID();

        List<BulkOrderJobItem> items =
                persistedItems(
                        jobId,
                        1
                );

        when(itemService.findItems(jobId))
                .thenReturn(items);

        when(itemService.claimItem(
                jobId,
                0
        )).thenReturn(true);

        when(orderService.createOrder(
                any(CreateOrderRequest.class),
                anyString()
        )).thenThrow(
                new ResourceNotFoundException(
                        "customer not found"
                )
        );

        processor.process(jobId);

        verify(
                itemService
        ).markFailed(
                eq(jobId),
                eq(0),
                contains("customer not found")
        );

        verify(
                progressService
        ).markItemFailed(
                eq(jobId),
                contains("customer not found")
        );
    }

    private List<BulkOrderJobItem> persistedItems(
            UUID jobId,
            int count
    ) {

        return java.util.stream.IntStream
                .range(0, count)
                .mapToObj(index ->
                        new BulkOrderJobItem(
                                jobId,
                                index,
                                payload()
                        )
                )
                .toList();
    }

    private String payload() {

        BulkCreateOrderRequest.BulkOrderItem item =
                new BulkCreateOrderRequest.BulkOrderItem(
                        UUID.randomUUID(),
                        "INR",
                        List.of(
                                new CreateOrderItemRequest(
                                        UUID.randomUUID(),
                                        1,
                                        new BigDecimal("100.00")
                                )
                        )
                );

        try {
            return jsonMapper.writeValueAsString(item);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to create test payload",
                    ex
            );
        }
    }
}
