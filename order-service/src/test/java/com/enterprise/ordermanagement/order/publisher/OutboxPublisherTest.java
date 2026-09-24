package com.enterprise.ordermanagement.order.publisher;

import com.enterprise.ordermanagement.order.entity.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxPublisherTest {

    private OutboxEventClaimService claimService;
    private KafkaEventPublisher kafkaEventPublisher;
    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        claimService = mock(OutboxEventClaimService.class);
        kafkaEventPublisher = mock(KafkaEventPublisher.class);

        publisher = new OutboxPublisher(
                claimService,
                kafkaEventPublisher
        );
    }

    @Test
    void publisherShouldRecoverClaimAndPublishEvents() {
        OutboxEvent event = new OutboxEvent(
                "ORDER",
                UUID.randomUUID(),
                "OrderCreated",
                "{\"eventType\":\"OrderCreated\"}"
        );

        when(claimService.claimPendingEvents(any(UUID.class)))
                .thenReturn(List.of(event));

        when(kafkaEventPublisher.publish(
                event.getAggregateId().toString(),
                event.getPayload()
        )).thenReturn(
                CompletableFuture.completedFuture(null)
        );

        publisher.publishPendingEvents();

        verify(claimService).recoverStuckEvents();

        verify(claimService).claimPendingEvents(any(UUID.class));

        verify(kafkaEventPublisher).publish(
                event.getAggregateId().toString(),
                event.getPayload()
        );

        verify(claimService).markPublished(
                eq(event.getId()),
                any(UUID.class)
        );

        verify(claimService, never()).handleFailure(
                any(UUID.class),
                any(UUID.class),
                anyInt()
        );
    }

    @Test
    void publisherShouldHandleKafkaFailureAsRetry() {
        OutboxEvent event = new OutboxEvent(
                "ORDER",
                UUID.randomUUID(),
                "OrderCreated",
                "{\"eventType\":\"OrderCreated\"}"
        );

        when(claimService.claimPendingEvents(any(UUID.class)))
                .thenReturn(List.of(event));

        when(kafkaEventPublisher.publish(
                event.getAggregateId().toString(),
                event.getPayload()
        )).thenReturn(
                CompletableFuture.failedFuture(
                        new RuntimeException("Kafka unavailable")
                )
        );

        publisher.publishPendingEvents();

        verify(claimService).recoverStuckEvents();

        verify(claimService).claimPendingEvents(any(UUID.class));

        verify(claimService).handleFailure(
                eq(event.getId()),
                any(UUID.class),
                eq(5)
        );

        verify(claimService, never()).markPublished(
                any(UUID.class),
                any(UUID.class)
        );
    }

    @Test
    void publisherShouldDoNothingWhenNoEventsAreClaimed() {
        when(claimService.claimPendingEvents(any(UUID.class)))
                .thenReturn(List.of());

        publisher.publishPendingEvents();

        verify(claimService).recoverStuckEvents();

        verify(claimService).claimPendingEvents(any(UUID.class));

        verifyNoInteractions(kafkaEventPublisher);
    }
}
