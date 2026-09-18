package com.enterprise.ordermanagement.order.publisher;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class KafkaEventPublisher {

    private static final String ORDERS_EVENTS_TOPIC = "orders.events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<?> publish(
            String orderId,
            String eventJson
    ) {
        return kafkaTemplate.send(
                ORDERS_EVENTS_TOPIC,
                orderId,
                eventJson
        );
    }
}
