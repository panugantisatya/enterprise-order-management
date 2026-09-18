package com.enterprise.ordermanagement.order;

import com.enterprise.ordermanagement.order.publisher.KafkaEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class KafkaEventPublisherTest {

    @Autowired
    private KafkaEventPublisher kafkaEventPublisher;

    @Test
    void shouldPublishEventToKafka() {
        String orderId = UUID.randomUUID().toString();

        String eventJson = """
                {
                  "eventId": "%s",
                  "eventType": "ProducerTest",
                  "message": "Kafka producer isolation test"
                }
                """.formatted(UUID.randomUUID());

        var result = kafkaEventPublisher
                .publish(orderId, eventJson)
                .join();

        assertNotNull(result);
    }
}
