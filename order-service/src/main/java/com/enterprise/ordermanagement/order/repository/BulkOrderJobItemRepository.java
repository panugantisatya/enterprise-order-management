package com.enterprise.ordermanagement.order.repository;

import com.enterprise.ordermanagement.order.entity.BulkOrderJobItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BulkOrderJobItemRepository
        extends JpaRepository<BulkOrderJobItem, UUID> {

    Optional<BulkOrderJobItem> findByJobIdAndItemIndex(
            UUID jobId,
            Integer itemIndex
    );

    List<BulkOrderJobItem> findByJobIdOrderByItemIndex(
            UUID jobId
    );

    @Modifying
    @Query(value = """
            UPDATE bulk_order_job_items
            SET status = 'PROCESSING',
                processing_started_at = NULL,
                completed_at = NULL,
                order_id = NULL,
                error_message = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE job_id = :jobId
              AND status = 'FAILED'
            """, nativeQuery = true)
    int requeueFailedItems(
            @Param("jobId") UUID jobId
    );
}
