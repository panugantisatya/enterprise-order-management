package com.enterprise.ordermanagement.order.consumer;

import com.enterprise.ordermanagement.order.bulk.BulkOrderJobCreatedEvent;
import com.enterprise.ordermanagement.order.service.BulkOrderJobProcessor;
import com.enterprise.ordermanagement.order.service.ConsumerIdempotencyService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Component
public class OrderEventConsumer {

    private static final String ORDERS_EVENTS_TOPIC =
            "orders.events";

    private static final String CONSUMER_GROUP =
            "order-service-consumer";

    private final ConsumerIdempotencyService consumerIdempotencyService;
    private final BulkOrderJobProcessor bulkOrderJobProcessor;
    private final JsonMapper jsonMapper;

    public OrderEventConsumer(
            ConsumerIdempotencyService consumerIdempotencyService,
            BulkOrderJobProcessor bulkOrderJobProcessor,
            JsonMapper jsonMapper
    ) {
        this.consumerIdempotencyService = consumerIdempotencyService;
        this.bulkOrderJobProcessor = bulkOrderJobProcessor;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(
            topics = ORDERS_EVENTS_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "3"
    )
    public void consume(String eventJson) {

        try {
            JsonNode root = jsonMapper.readTree(eventJson);

            UUID eventId = extractEventId(root);
            String eventType = extractEventType(root);

            if (eventType == null || eventType.isBlank()) {
                markProcessed(eventId);
                return;
            }

            if (consumerIdempotencyService.alreadyProcessed(
                    CONSUMER_GROUP,
                    eventId
            )) {
                return;
            }

            switch (eventType) {

                case "OrderCreated" ->
                        processOrderCreated(eventJson, eventId);

                case "BulkOrderJobCreated" ->
                        processBulkOrderJobCreated(root);

                default -> {
                    // Unsupported events are intentionally ignored.
                }
            }

            markProcessed(eventId);

        } catch (JacksonException | IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Invalid order event payload",
                    ex
            );
        }
    }

    private void processOrderCreated(
            String eventJson,
            UUID eventId
    ) {
        System.out.println(
                "Received OrderCreated event: " + eventId
        );
    }

    private void processBulkOrderJobCreated(
            JsonNode root
    ) throws JacksonException {

        JsonNode jobIdNode = root.get("jobId");
        JsonNode ordersNode = root.get("orders");

        if (jobIdNode == null || jobIdNode.isNull()) {
            throw new IllegalArgumentException(
                    "BulkOrderJobCreated event missing jobId"
            );
        }

        if (ordersNode == null || !ordersNode.isArray()) {
            throw new IllegalArgumentException(
                    "BulkOrderJobCreated event missing orders"
            );
        }

        BulkOrderJobCreatedEvent event =
                jsonMapper.treeToValue(
                        root,
                        BulkOrderJobCreatedEvent.class
                );

        bulkOrderJobProcessor.process(
                event.jobId(),
                event.orders()
        );
    }

    private UUID extractEventId(JsonNode root) {

        JsonNode eventIdNode = root.get("eventId");

        if (eventIdNode == null || eventIdNode.isNull()) {
            throw new IllegalArgumentException(
                    "Event missing eventId"
            );
        }

        return UUID.fromString(
                eventIdNode.asText()
        );
    }

    private String extractEventType(JsonNode root) {

        JsonNode eventTypeNode = root.get("eventType");

        if (eventTypeNode == null || eventTypeNode.isNull()) {
            return null;
        }

        return eventTypeNode.asText();
    }

    private void markProcessed(UUID eventId) {

        consumerIdempotencyService.markProcessed(
                CONSUMER_GROUP,
                eventId
        );
    }
}
