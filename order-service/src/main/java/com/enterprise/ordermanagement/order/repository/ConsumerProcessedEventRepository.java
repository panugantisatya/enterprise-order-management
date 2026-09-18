package com.enterprise.ordermanagement.order.repository;

import com.enterprise.ordermanagement.order.entity.ConsumerProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsumerProcessedEventRepository
        extends JpaRepository<ConsumerProcessedEvent, UUID> {

    boolean existsByConsumerGroupAndEventId(
            String consumerGroup,
            UUID eventId
    );
}
