package com.enterprise.ordermanagement.order.publisher;

import com.enterprise.ordermanagement.order.entity.OutboxEvent;
import com.enterprise.ordermanagement.order.entity.OutboxEvent.OutboxEventStatus;
import com.enterprise.ordermanagement.order.repository.OutboxEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OutboxPublisher {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_SECONDS = 30;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaEventPublisher kafkaEventPublisher;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaEventPublisher kafkaEventPublisher
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaEventPublisher = kafkaEventPublisher;
    }

    @Scheduled(
            fixedDelayString = "${outbox.publisher.fixed-delay-ms:5000}"
    )
    public void publishPendingEvents() {

        List<OutboxEvent> events = claimPendingEvents();

        for (OutboxEvent event : events) {
            publishEvent(event);
        }
    }

    @Transactional
    protected List<OutboxEvent> claimPendingEvents() {

        List<OutboxEvent> events =
                outboxEventRepository.findReadyEvents(
                        OutboxEventStatus.PENDING,
                        Instant.now(),
                        PageRequest.of(0, BATCH_SIZE)
                );

        if (events.isEmpty()) {
            return events;
        }

        List<UUID> eventIds = events.stream()
                .map(OutboxEvent::getId)
                .toList();

        int claimed =
                outboxEventRepository.claimEvents(
                        eventIds,
                        OutboxEventStatus.PENDING,
                        OutboxEventStatus.PROCESSING
                );

        if (claimed == 0) {
            return List.of();
        }

        return outboxEventRepository.findAllById(eventIds)
                .stream()
                .filter(event ->
                        event.getStatus() == OutboxEventStatus.PROCESSING
                )
                .toList();
    }

    private void publishEvent(OutboxEvent event) {

        try {

            kafkaEventPublisher
                    .publish(
                            event.getAggregateId().toString(),
                            event.getPayload()
                    )
                    .join();

            markPublished(event.getId());

        } catch (Exception ex) {

            handlePublishFailure(event, ex);
        }
    }

    @Transactional
    protected void markPublished(UUID eventId) {

        OutboxEvent event =
                outboxEventRepository.findById(eventId)
                        .orElseThrow();

        event.markPublished();
    }

    @Transactional
    protected void handlePublishFailure(
            OutboxEvent event,
            Exception exception
    ) {

        OutboxEvent current =
                outboxEventRepository.findById(event.getId())
                        .orElseThrow();

        if (current.getRetryCount() + 1 >= MAX_RETRIES) {

            current.markFailed();

        } else {

            current.markRetry(
                    Instant.now().plusSeconds(RETRY_DELAY_SECONDS)
            );
        }
    }
}
