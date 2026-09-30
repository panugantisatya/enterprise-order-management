package com.enterprise.ordermanagement.order.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BulkProcessingConfigTest {

    @Test
    void shouldCreateBoundedBulkExecutor() {

        BulkProcessingConfig config =
                new BulkProcessingConfig();

        ThreadPoolTaskExecutor executor =
                config.bulkTaskExecutor(
                        4,
                        8
                );

        assertEquals(
                4,
                executor.getCorePoolSize()
        );

        assertEquals(
                4,
                executor.getMaxPoolSize()
        );

        executor.shutdown();
    }
}
