package com.enterprise.ordermanagement.order.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private static final String ORDERS_EVENTS_TOPIC = "orders.events";

    @KafkaListener(
            topics = ORDERS_EVENTS_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(
            String eventJson
    ) {
        System.out.println(
                "Received order event: " + eventJson
        );
    }
}
