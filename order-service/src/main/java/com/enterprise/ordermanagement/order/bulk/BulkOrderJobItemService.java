package com.enterprise.ordermanagement.order.bulk;

import com.enterprise.ordermanagement.order.dto.BulkCreateOrderRequest;
import com.enterprise.ordermanagement.order.entity.BulkOrderJobItem;
import com.enterprise.ordermanagement.order.entity.BulkOrderJobItem.BulkOrderJobItemStatus;
import com.enterprise.ordermanagement.order.repository.BulkOrderJobItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BulkOrderJobItemService {

    private static final Duration PROCESSING_TIMEOUT =
            Duration.ofMinutes(5);

    private final BulkOrderJobItemRepository repository;
    private final JsonMapper jsonMapper;

    public BulkOrderJobItemService(
            BulkOrderJobItemRepository repository,
            JsonMapper jsonMapper
    ) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    /*
     * IMPORTANT:
     * This deliberately participates in the caller transaction.
     *
     * BulkOrderJob + BulkOrderJobItems + OutboxEvent must commit
     * atomically.
     */
    @Transactional
    public void createItems(
            UUID jobId,
            List<BulkCreateOrderRequest.BulkOrderItem> orders
    ) {

        for (int index = 0; index < orders.size(); index++) {

            String payload =
                    jsonMapper.writeValueAsString(
                            orders.get(index)
                    );

            repository.save(
                    new BulkOrderJobItem(
                            jobId,
                            index,
                            payload
                    )
            );
        }
    }

    @Transactional(readOnly = true)
    public List<BulkOrderJobItem> findItems(
            UUID jobId
    ) {
        return repository.findByJobIdOrderByItemIndex(jobId);
    }

    @Transactional
    public int requeueFailedItems(UUID jobId) {
        return repository.requeueFailedItems(jobId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimItem(
            UUID jobId,
            int itemIndex
    ) {

        var existing =
                repository.findByJobIdAndItemIndex(
                        jobId,
                        itemIndex
                );

        if (existing.isEmpty()) {
            return false;
        }

        BulkOrderJobItem item =
                existing.get();

        if (item.getStatus() ==
                BulkOrderJobItemStatus.SUCCEEDED) {
            return false;
        }

        if (item.getStatus() ==
                BulkOrderJobItemStatus.FAILED) {
            return false;
        }

        Instant cutoff =
                Instant.now().minus(
                        PROCESSING_TIMEOUT
                );

        if (item.getStatus() ==
                BulkOrderJobItemStatus.PROCESSING) {

            /*
             * Initial durable item created by the bulk job.
             *
             * The item is PROCESSING by schema design, but a
             * null processingStartedAt means no worker has
             * actually claimed it yet.
             */
            if (item.getProcessingStartedAt() == null) {
                item.reclaim();
                return true;
            }

            /*
             * Existing 12F recovery boundary.
             */
            if (item.isProcessingStale(cutoff)) {
                item.reclaim();
                return true;
            }

            return false;
        }

        return false;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSucceeded(
            UUID jobId,
            int itemIndex,
            UUID orderId
    ) {

        repository.findByJobIdAndItemIndex(
                        jobId,
                        itemIndex
                )
                .ifPresent(item ->
                        item.markSucceeded(orderId)
                );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(
            UUID jobId,
            int itemIndex,
            String errorMessage
    ) {

        repository.findByJobIdAndItemIndex(
                        jobId,
                        itemIndex
                )
                .ifPresent(item ->
                        item.markFailed(errorMessage)
                );
    }
}
