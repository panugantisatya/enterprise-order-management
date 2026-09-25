package com.enterprise.ordermanagement.order.bulk;

import com.enterprise.ordermanagement.order.entity.BulkOrderJob;
import com.enterprise.ordermanagement.order.repository.BulkOrderJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BulkOrderJobProgressService {

    private final BulkOrderJobRepository bulkOrderJobRepository;

    public BulkOrderJobProgressService(
            BulkOrderJobRepository bulkOrderJobRepository
    ) {
        this.bulkOrderJobRepository = bulkOrderJobRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessing(UUID jobId) {
        BulkOrderJob job = getJob(jobId);

        if (isTerminal(job)) {
            return;
        }

        job.markProcessing();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markItemSucceeded(UUID jobId) {
        BulkOrderJob job = getJob(jobId);

        if (isTerminal(job)) {
            return;
        }

        job.markItemSucceeded();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markItemFailed(
            UUID jobId,
            String errorMessage
    ) {
        BulkOrderJob job = getJob(jobId);

        if (isTerminal(job)) {
            return;
        }

        job.markItemFailed(errorMessage);
    }

    private BulkOrderJob getJob(UUID jobId) {
        return bulkOrderJobRepository.findById(jobId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Bulk order job not found: " + jobId
                        )
                );
    }

    private boolean isTerminal(BulkOrderJob job) {
        return job.getStatus() == BulkOrderJob.BulkOrderJobStatus.COMPLETED
                || job.getStatus() == BulkOrderJob.BulkOrderJobStatus.COMPLETED_WITH_ERRORS
                || job.getStatus() == BulkOrderJob.BulkOrderJobStatus.FAILED;
    }
}
