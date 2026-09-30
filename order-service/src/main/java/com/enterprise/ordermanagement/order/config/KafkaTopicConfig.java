package com.enterprise.ordermanagement.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    private static final String ORDERS_EVENTS_DLT =
            "orders.events.DLT";

    private static final int DLT_PARTITIONS = 3;

    private static final short DLT_REPLICATION_FACTOR = 1;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(
                org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic ordersEventsDltTopic() {
        return new NewTopic(
                ORDERS_EVENTS_DLT,
                DLT_PARTITIONS,
                DLT_REPLICATION_FACTOR
        );
    }
}
