package com.enterprise.ordermanagement.order.service;

import com.enterprise.ordermanagement.order.entity.ConsumerProcessedEvent;
import com.enterprise.ordermanagement.order.repository.ConsumerProcessedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ConsumerIdempotencyService {

    private final ConsumerProcessedEventRepository repository;

    public ConsumerIdempotencyService(
            ConsumerProcessedEventRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean alreadyProcessed(
            String consumerGroup,
            UUID eventId
    ) {
        return repository.existsByConsumerGroupAndEventId(
                consumerGroup,
                eventId
        );
    }

    @Transactional
    public void markProcessed(
            String consumerGroup,
            UUID eventId
    ) {
        repository.save(
                new ConsumerProcessedEvent(
                        consumerGroup,
                        eventId
                )
        );
    }
}
