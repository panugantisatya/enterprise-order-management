package com.enterprise.ordermanagement.order.publisher;

import com.enterprise.ordermanagement.order.entity.OutboxEvent;
import com.enterprise.ordermanagement.order.entity.OutboxEvent.OutboxEventStatus;
import com.enterprise.ordermanagement.order.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxEventClaimServiceTest {

    private OutboxEventRepository repository;
    private OutboxEventClaimService service;

    @BeforeEach
    void setUp() {
        repository = mock(OutboxEventRepository.class);
        service = new OutboxEventClaimService(repository);
    }

    @Test
    void claimPendingEventsShouldClaimReadyEvents() {
        OutboxEvent event = new OutboxEvent(
                "ORDER",
                UUID.randomUUID(),
                "OrderCreated",
                "{}"
        );

        UUID processingToken = UUID.randomUUID();

        when(repository.findReadyEvents(
                eq(OutboxEventStatus.PENDING),
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(List.of(event));

        when(repository.claimEvents(
                anyList(),
                eq(OutboxEventStatus.PENDING),
                eq(OutboxEventStatus.PROCESSING),
                eq(processingToken),
                any(Instant.class)
        )).thenReturn(1);

        when(repository.findByProcessingToken(processingToken))
                .thenReturn(List.of(event));

        List<OutboxEvent> claimed =
                service.claimPendingEvents(processingToken);

        assertEquals(1, claimed.size());
        assertSame(event, claimed.get(0));

        verify(repository).claimEvents(
                eq(List.of(event.getId())),
                eq(OutboxEventStatus.PENDING),
                eq(OutboxEventStatus.PROCESSING),
                eq(processingToken),
                any(Instant.class)
        );

        verify(repository).findByProcessingToken(processingToken);
    }

    @Test
    void claimPendingEventsShouldReturnEmptyWhenNothingIsReady() {
        UUID processingToken = UUID.randomUUID();

        when(repository.findReadyEvents(
                eq(OutboxEventStatus.PENDING),
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(List.of());

        List<OutboxEvent> claimed =
                service.claimPendingEvents(processingToken);

        assertTrue(claimed.isEmpty());

        verify(repository, never()).claimEvents(
                anyList(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void claimPendingEventsShouldReturnEmptyWhenClaimLosesRace() {
        OutboxEvent event = new OutboxEvent(
                "ORDER",
                UUID.randomUUID(),
                "OrderCreated",
                "{}"
        );

        UUID processingToken = UUID.randomUUID();

        when(repository.findReadyEvents(
                eq(OutboxEventStatus.PENDING),
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(List.of(event));

        when(repository.claimEvents(
                anyList(),
                eq(OutboxEventStatus.PENDING),
                eq(OutboxEventStatus.PROCESSING),
                eq(processingToken),
                any(Instant.class)
        )).thenReturn(0);

        List<OutboxEvent> claimed =
                service.claimPendingEvents(processingToken);

        assertTrue(claimed.isEmpty());

        verify(repository, never())
                .findByProcessingToken(processingToken);
    }

    @Test
    void markPublishedShouldPublishWhenTokenMatches() {
        OutboxEvent event = new OutboxEvent(
                "ORDER",
                UUID.randomUUID(),
                "OrderCreated",
                "{}"
        );

        UUID token = UUID.randomUUID();

        event.markProcessing(token);

        when(repository.findById(event.getId()))
                .thenReturn(java.util.Optional.of(event));

        service.markPublished(event.getId(), token);

        assertEquals(
                OutboxEventStatus.PUBLISHED,
                event.getStatus()
        );
    }

    @Test
    void markPublishedShouldIgnoreWrongToken() {
        OutboxEvent event = new OutboxEvent(
                "ORDER",
                UUID.randomUUID(),
                "OrderCreated",
                "{}"
        );

        UUID correctToken = UUID.randomUUID();
        UUID wrongToken = UUID.randomUUID();

        event.markProcessing(correctToken);

        when(repository.findById(event.getId()))
                .thenReturn(java.util.Optional.of(event));

        service.markPublished(event.getId(), wrongToken);

        assertEquals(
                OutboxEventStatus.PROCESSING,
                event.getStatus()
        );
    }

    @Test
    void handleFailureShouldScheduleRetryBeforeMaximumRetries() {
        OutboxEvent event = new OutboxEvent(
                "ORDER",
                UUID.randomUUID(),
                "OrderCreated",
                "{}"
        );

        UUID token = UUID.randomUUID();

        event.markProcessing(token);

        when(repository.findById(event.getId()))
                .thenReturn(java.util.Optional.of(event));

        service.handleFailure(
                event.getId(),
                token,
                5
        );

        assertEquals(
                OutboxEventStatus.PENDING,
                event.getStatus()
        );

        assertEquals(1, event.getRetryCount());
        assertNotNull(event.getNextAttemptAt());
    }

    @Test
    void handleFailureShouldMarkFailedAtMaximumRetries() {
        OutboxEvent event = new OutboxEvent(
                "ORDER",
                UUID.randomUUID(),
                "OrderCreated",
                "{}"
        );

        UUID token = UUID.randomUUID();

        event.markProcessing(token);

        when(repository.findById(event.getId()))
                .thenReturn(java.util.Optional.of(event));

        service.handleFailure(
                event.getId(),
                token,
                1
        );

        assertEquals(
                OutboxEventStatus.FAILED,
                event.getStatus()
        );

        assertEquals(1, event.getRetryCount());
        assertNull(event.getNextAttemptAt());
    }

    @Test
    void handleFailureShouldIgnoreWrongProcessingToken() {
        OutboxEvent event = new OutboxEvent(
                "ORDER",
                UUID.randomUUID(),
                "OrderCreated",
                "{}"
        );

        UUID correctToken = UUID.randomUUID();
        UUID wrongToken = UUID.randomUUID();

        event.markProcessing(correctToken);

        when(repository.findById(event.getId()))
                .thenReturn(java.util.Optional.of(event));

        service.handleFailure(
                event.getId(),
                wrongToken,
                5
        );

        assertEquals(
                OutboxEventStatus.PROCESSING,
                event.getStatus()
        );

        assertEquals(0, event.getRetryCount());
    }
}
