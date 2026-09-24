package com.enterprise.ordermanagement.order.publisher;

import com.enterprise.ordermanagement.order.entity.OutboxEvent;
import com.enterprise.ordermanagement.order.entity.OutboxEvent.OutboxEventStatus;
import com.enterprise.ordermanagement.order.repository.OutboxEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OutboxEventClaimService {

    private static final int BATCH_SIZE = 100;

    private static final long RETRY_DELAY_SECONDS = 30;
    private static final long PROCESSING_TIMEOUT_SECONDS = 120;

    private final OutboxEventRepository repository;

    public OutboxEventClaimService(
            OutboxEventRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public List<OutboxEvent> claimPendingEvents(
            UUID processingToken
    ) {

        Instant now = Instant.now();

        List<OutboxEvent> events =
                repository.findReadyEvents(
                        OutboxEventStatus.PENDING,
                        now,
                        PageRequest.of(0, BATCH_SIZE)
                );

        if (events.isEmpty()) {
            return List.of();
        }

        List<UUID> eventIds = events.stream()
                .map(OutboxEvent::getId)
                .toList();

        int claimed =
                repository.claimEvents(
                        eventIds,
                        OutboxEventStatus.PENDING,
                        OutboxEventStatus.PROCESSING,
                        processingToken,
                        now
                );

        if (claimed == 0) {
            return List.of();
        }

        return repository.findByProcessingToken(processingToken);
    }

    @Transactional
    public void recoverStuckEvents() {

        Instant now = Instant.now();

        Instant cutoff =
                now.minusSeconds(PROCESSING_TIMEOUT_SECONDS);

        Instant nextAttempt =
                now.plusSeconds(RETRY_DELAY_SECONDS);

        repository.recoverStuckEvents(
                OutboxEventStatus.PROCESSING,
                OutboxEventStatus.PENDING,
                cutoff,
                nextAttempt
        );
    }

    @Transactional
    public void markPublished(
            UUID eventId,
            UUID processingToken
    ) {

        repository.findById(eventId)
                .ifPresent(event -> {

                    if (event.getStatus()
                            != OutboxEventStatus.PROCESSING) {
                        return;
                    }

                    if (!processingToken.equals(
                            event.getProcessingToken())) {
                        return;
                    }

                    event.markPublished();
                });
    }

    @Transactional
    public void handleFailure(
            UUID eventId,
            UUID processingToken,
            int maxRetries
    ) {

        repository.findById(eventId)
                .ifPresent(event -> {

                    if (event.getStatus()
                            != OutboxEventStatus.PROCESSING) {
                        return;
                    }

                    if (!processingToken.equals(
                            event.getProcessingToken())) {
                        return;
                    }

                    if (event.getRetryCount() + 1 >= maxRetries) {

                        event.markFailed();

                    } else {

                        event.markRetry(
                                Instant.now().plusSeconds(
                                        RETRY_DELAY_SECONDS
                                )
                        );
                    }
                });
    }
}
