package com.enterprise.ordermanagement.order.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OutboxEventTest {

    @Test
    void newEventShouldStartAsPending() {
        OutboxEvent event = new OutboxEvent(
                "ORDER",
                UUID.randomUUID(),
                "OrderCreated",
                "{\"eventType\":\"OrderCreated\"}"
        );

        assertEquals(
                OutboxEvent.OutboxEventStatus.PENDING,
                event.getStatus()
        );
        assertEquals(0, event.getRetryCount());
        assertNotNull(event.getNextAttemptAt());
        assertNotNull(event.getId());
    }

    @Test
    void markProcessingShouldSetProcessingMetadata() {
        OutboxEvent event = new OutboxEvent(
                "ORDER",
                UUID.randomUUID(),
                "OrderCreated",
                "{}"
        );

        UUID processingToken = UUID.randomUUID();

        event.markProcessing(processingToken);

        assertEquals(
                OutboxEvent.OutboxEventStatus.PROCESSING,
                event.getStatus()
        );
        assertEquals(processingToken, event.getProcessingToken());
        assertNotNull(event.getProcessingStartedAt());
    }

    @Test
    void markPublishedShouldClearProcessingMetadata() {
        OutboxEvent event = new OutboxEvent(
                "ORDER",
                UUID.randomUUID(),
                "OrderCreated",
                "{}"
        );

        UUID processingToken = UUID.randomUUID();

        event.markProcessing(processingToken);
        event.markPublished();

        assertEquals(
                OutboxEvent.OutboxEventStatus.PUBLISHED,
                event.getStatus()
        );
        assertNotNull(event.getPublishedAt());
        assertNull(event.getNextAttemptAt());
        assertNull(event.getProcessingToken());
        assertNull(event.getProcessingStartedAt());
    }

    @Test
    void markRetryShouldIncrementRetryCountAndReturnToPending() {
        OutboxEvent event = new OutboxEvent(
                "ORDER",
                UUID.randomUUID(),
                "OrderCreated",
                "{}"
        );

        UUID processingToken = UUID.randomUUID();

        event.markProcessing(processingToken);

        Instant retryAt = Instant.now().plusSeconds(30);

        event.markRetry(retryAt);

        assertEquals(
                OutboxEvent.OutboxEventStatus.PENDING,
                event.getStatus()
        );
        assertEquals(1, event.getRetryCount());
        assertEquals(retryAt, event.getNextAttemptAt());
        assertNull(event.getProcessingToken());
        assertNull(event.getProcessingStartedAt());
    }

    @Test
    void markFailedShouldIncrementRetryCountAndClearProcessingMetadata() {
        OutboxEvent event = new OutboxEvent(
                "ORDER",
                UUID.randomUUID(),
                "OrderCreated",
                "{}"
        );

        UUID processingToken = UUID.randomUUID();

        event.markProcessing(processingToken);
        event.markFailed();

        assertEquals(
                OutboxEvent.OutboxEventStatus.FAILED,
                event.getStatus()
        );
        assertEquals(1, event.getRetryCount());
        assertNull(event.getNextAttemptAt());
        assertNull(event.getProcessingToken());
        assertNull(event.getProcessingStartedAt());
    }
}
