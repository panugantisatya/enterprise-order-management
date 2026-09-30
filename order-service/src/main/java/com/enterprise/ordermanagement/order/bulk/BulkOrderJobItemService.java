package com.enterprise.ordermanagement.order.bulk;

import com.enterprise.ordermanagement.order.entity.BulkOrderJobItem;
import com.enterprise.ordermanagement.order.entity.BulkOrderJobItem.BulkOrderJobItemStatus;
import com.enterprise.ordermanagement.order.repository.BulkOrderJobItemRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class BulkOrderJobItemService {

    private static final Duration PROCESSING_TIMEOUT =
            Duration.ofMinutes(5);

    private final BulkOrderJobItemRepository repository;

    public BulkOrderJobItemService(
            BulkOrderJobItemRepository repository
    ) {
        this.repository = repository;
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
            try {
                repository.saveAndFlush(
                        new BulkOrderJobItem(
                                jobId,
                                itemIndex
                        )
                );
                return true;
            } catch (DataIntegrityViolationException ex) {
                return false;
            }
        }

        BulkOrderJobItem item = existing.get();

        if (item.getStatus() ==
                BulkOrderJobItemStatus.SUCCEEDED) {
            return false;
        }

        if (item.getStatus() ==
                BulkOrderJobItemStatus.FAILED) {
            return false;
        }

        Instant cutoff =
                Instant.now().minus(PROCESSING_TIMEOUT);

        if (item.isProcessingStale(cutoff)) {
            item.reclaim();
            return true;
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
