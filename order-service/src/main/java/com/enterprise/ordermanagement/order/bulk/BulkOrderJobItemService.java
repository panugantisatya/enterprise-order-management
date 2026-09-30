package com.enterprise.ordermanagement.order.bulk;

import com.enterprise.ordermanagement.order.entity.BulkOrderJobItem;
import com.enterprise.ordermanagement.order.repository.BulkOrderJobItemRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BulkOrderJobItemService {

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

        if (repository.findByJobIdAndItemIndex(
                jobId,
                itemIndex
        ).isPresent()) {
            return false;
        }

        try {
            repository.saveAndFlush(
                    new BulkOrderJobItem(
                            jobId,
                            itemIndex
                    )
            );

            return true;

        } catch (DataIntegrityViolationException ex) {
            /*
             * Another consumer/thread won the claim.
             * The unique (job_id, item_index) constraint
             * makes this safe across multiple instances.
             */
            return false;
        }
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
