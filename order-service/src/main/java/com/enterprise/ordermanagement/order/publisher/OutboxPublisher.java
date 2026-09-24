package com.enterprise.ordermanagement.order.publisher;

import com.enterprise.ordermanagement.order.entity.OutboxEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class OutboxPublisher {

    private static final int MAX_RETRIES = 5;

    private final OutboxEventClaimService claimService;
    private final KafkaEventPublisher kafkaEventPublisher;

    public OutboxPublisher(
            OutboxEventClaimService claimService,
            KafkaEventPublisher kafkaEventPublisher
    ) {
        this.claimService = claimService;
        this.kafkaEventPublisher = kafkaEventPublisher;
    }

    @Scheduled(
            fixedDelayString = "${outbox.publisher.fixed-delay-ms:5000}"
    )
    public void publishPendingEvents() {

        claimService.recoverStuckEvents();

        UUID processingToken = UUID.randomUUID();

        List<OutboxEvent> events =
                claimService.claimPendingEvents(
                        processingToken
                );

        for (OutboxEvent event : events) {
            publishEvent(event, processingToken);
        }
    }

    private void publishEvent(
            OutboxEvent event,
            UUID processingToken
    ) {

        try {

            kafkaEventPublisher
                    .publish(
                            event.getAggregateId().toString(),
                            event.getPayload()
                    )
                    .join();

            claimService.markPublished(
                    event.getId(),
                    processingToken
            );

        } catch (Exception ex) {

            claimService.handleFailure(
                    event.getId(),
                    processingToken,
                    MAX_RETRIES
            );
        }
    }
}
