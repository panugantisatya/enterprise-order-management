package com.enterprise.ordermanagement.order.consumer;

import com.enterprise.ordermanagement.order.bulk.BulkOrderJobCreatedEvent;
import com.enterprise.ordermanagement.order.exception.InvalidOrderEventException;
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
        this.consumerIdempotencyService =
                consumerIdempotencyService;
        this.bulkOrderJobProcessor =
                bulkOrderJobProcessor;
        this.jsonMapper =
                jsonMapper;
    }

    @KafkaListener(
            topics = ORDERS_EVENTS_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "3"
    )
    public void consume(String eventJson) {

        final JsonNode root;

        try {
            root =
                    jsonMapper.readTree(eventJson);
        } catch (JacksonException ex) {
            throw new InvalidOrderEventException(
                    "Invalid order event JSON",
                    ex
            );
        }

        try {

            UUID eventId =
                    extractEventId(root);

            String eventType =
                    extractEventType(root);

            if (eventType == null
                    || eventType.isBlank()) {

                throw new InvalidOrderEventException(
                        "Event missing eventType"
                );
            }

            if (consumerIdempotencyService
                    .alreadyProcessed(
                            CONSUMER_GROUP,
                            eventId
                    )) {
                return;
            }

            switch (eventType) {

                case "OrderCreated" ->
                        processOrderCreated(
                                eventId
                        );

                case "BulkOrderJobCreated" ->
                        processBulkOrderJobCreated(
                                root
                        );

                default -> {
                    // Unsupported events are intentionally ignored.
                }
            }

            markProcessed(eventId);

        } catch (InvalidOrderEventException ex) {
            throw ex;

        } catch (IllegalArgumentException ex) {
            throw new InvalidOrderEventException(
                    "Invalid order event",
                    ex
            );

        } catch (JacksonException ex) {
            throw new InvalidOrderEventException(
                    "Invalid order event payload",
                    ex
            );
        }
    }

    private void processOrderCreated(
            UUID eventId
    ) {
        System.out.println(
                "Received OrderCreated event: "
                        + eventId
        );
    }

    private void processBulkOrderJobCreated(
            JsonNode root
    ) throws JacksonException {

        JsonNode jobIdNode =
                root.get("jobId");

        if (jobIdNode == null
                || jobIdNode.isNull()) {

            throw new InvalidOrderEventException(
                    "BulkOrderJobCreated event missing jobId"
            );
        }

        UUID jobId;

        try {
            jobId =
                    UUID.fromString(
                            jobIdNode.asText()
                    );
        } catch (IllegalArgumentException ex) {
            throw new InvalidOrderEventException(
                    "BulkOrderJobCreated event contains invalid jobId",
                    ex
            );
        }

        BulkOrderJobCreatedEvent event =
                jsonMapper.treeToValue(
                        root,
                        BulkOrderJobCreatedEvent.class
                );

        if (!jobId.equals(event.jobId())) {
            throw new InvalidOrderEventException(
                    "BulkOrderJobCreated event jobId mismatch"
            );
        }

        bulkOrderJobProcessor.process(
                event.jobId()
        );
    }

    private UUID extractEventId(
            JsonNode root
    ) {

        JsonNode eventIdNode =
                root.get("eventId");

        if (eventIdNode == null
                || eventIdNode.isNull()) {

            throw new InvalidOrderEventException(
                    "Event missing eventId"
            );
        }

        try {

            return UUID.fromString(
                    eventIdNode.asText()
            );

        } catch (IllegalArgumentException ex) {

            throw new InvalidOrderEventException(
                    "Event contains invalid eventId",
                    ex
            );
        }
    }

    private String extractEventType(
            JsonNode root
    ) {

        JsonNode eventTypeNode =
                root.get("eventType");

        if (eventTypeNode == null
                || eventTypeNode.isNull()) {
            return null;
        }

        return eventTypeNode.asText();
    }

    private void markProcessed(
            UUID eventId
    ) {

        consumerIdempotencyService.markProcessed(
                CONSUMER_GROUP,
                eventId
        );
    }
}
