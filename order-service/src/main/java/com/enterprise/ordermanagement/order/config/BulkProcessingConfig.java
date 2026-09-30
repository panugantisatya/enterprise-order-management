package com.enterprise.ordermanagement.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class BulkProcessingConfig {

    @Bean(name = "bulkTaskExecutor")
    public ThreadPoolTaskExecutor bulkTaskExecutor(
            @Value("${bulk.processing.worker-threads:4}")
            int workerThreads,

            @Value("${bulk.processing.queue-capacity:8}")
            int queueCapacity
    ) {

        if (workerThreads <= 0) {
            throw new IllegalArgumentException(
                    "bulk.processing.worker-threads must be greater than zero"
            );
        }

        if (queueCapacity <= 0) {
            throw new IllegalArgumentException(
                    "bulk.processing.queue-capacity must be greater than zero"
            );
        }

        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(workerThreads);
        executor.setMaxPoolSize(workerThreads);
        executor.setQueueCapacity(queueCapacity);

        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        executor.setThreadNamePrefix(
                "bulk-worker-"
        );

        executor.setWaitForTasksToCompleteOnShutdown(true);

        executor.initialize();

        return executor;
    }
}
