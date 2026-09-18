package com.enterprise.ordermanagement.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consumer_processed_events")
@Getter
@NoArgsConstructor
public class ConsumerProcessedEvent {

    @Id
    private UUID id;

    @Column(name = "consumer_group", nullable = false, length = 100)
    private String consumerGroup;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public ConsumerProcessedEvent(
            String consumerGroup,
            UUID eventId
    ) {
        this.id = UUID.randomUUID();
        this.consumerGroup = consumerGroup;
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (processedAt == null) {
            processedAt = Instant.now();
        }
    }
}
