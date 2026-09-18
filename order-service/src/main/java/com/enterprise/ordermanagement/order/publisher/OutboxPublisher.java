package com.enterprise.ordermanagement.order.publisher;

import com.enterprise.ordermanagement.order.entity.OutboxEvent;
import com.enterprise.ordermanagement.order.entity.OutboxEvent.OutboxEventStatus;
import com.enterprise.ordermanagement.order.repository.OutboxEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxPublisher {

    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaEventPublisher kafkaEventPublisher;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaEventPublisher kafkaEventPublisher
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaEventPublisher = kafkaEventPublisher;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events =
                outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                        OutboxEventStatus.PENDING,
                        PageRequest.of(0, BATCH_SIZE)
                );

        for (OutboxEvent event : events) {
            kafkaEventPublisher
                    .publish(
                            event.getAggregateId().toString(),
                            event.getPayload()
                    )
                    .join();

            event.markPublished();
            outboxEventRepository.save(event);
        }
    }
}
