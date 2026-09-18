package com.enterprise.ordermanagement.order.consumer;

import com.enterprise.ordermanagement.order.service.ConsumerIdempotencyService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Component
public class OrderEventConsumer {

    private static final String ORDERS_EVENTS_TOPIC = "orders.events";
    private static final String CONSUMER_GROUP = "order-service-consumer";

    private final ConsumerIdempotencyService consumerIdempotencyService;
    private final JsonMapper jsonMapper;

    public OrderEventConsumer(
            ConsumerIdempotencyService consumerIdempotencyService,
            JsonMapper jsonMapper
    ) {
        this.consumerIdempotencyService = consumerIdempotencyService;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(
            topics = ORDERS_EVENTS_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String eventJson) {

        try {
            JsonNode root = jsonMapper.readTree(eventJson);
            JsonNode eventIdNode = root.get("eventId");

            if (eventIdNode == null || eventIdNode.isNull()) {
                throw new IllegalArgumentException(
                        "eventId is missing from order event"
                );
            }

            UUID eventId = UUID.fromString(
                    eventIdNode.asString()
            );

            if (consumerIdempotencyService.alreadyProcessed(
                    CONSUMER_GROUP,
                    eventId
            )) {
                System.out.println(
                        "Skipping already processed event: " + eventId
                );
                return;
            }

            System.out.println(
                    "Processing order event: " + eventJson
            );

            consumerIdempotencyService.markProcessed(
                    CONSUMER_GROUP,
                    eventId
            );

        } catch (JacksonException | IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Invalid order event payload",
                    ex
            );
        }
    }
}
