package com.enterprise.ordermanagement.order.repository;

import com.enterprise.ordermanagement.order.entity.BulkOrderJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BulkOrderJobRepository
        extends JpaRepository<BulkOrderJob, UUID> {

    Optional<BulkOrderJob> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query(value = """
            UPDATE bulk_order_jobs
            SET processed_items = processed_items + 1,
                succeeded_items = succeeded_items + 1,
                status = CASE
                    WHEN processed_items + 1 >= total_items
                        THEN 'COMPLETED'
                    ELSE 'PROCESSING'
                END,
                completed_at = CASE
                    WHEN processed_items + 1 >= total_items
                        THEN CURRENT_TIMESTAMP
                    ELSE completed_at
                END,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :jobId
              AND status NOT IN ('COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED')
            """, nativeQuery = true)
    int incrementSucceededAndCompleteIfFinished(
            @Param("jobId") UUID jobId
    );

    @Modifying
    @Query(value = """
            UPDATE bulk_order_jobs
            SET processed_items = processed_items + 1,
                failed_items = failed_items + 1,
                error_message = CASE
                    WHEN error_message IS NULL
                         OR TRIM(error_message) = ''
                        THEN :errorMessage
                    ELSE error_message || '; ' || :errorMessage
                END,
                status = CASE
                    WHEN processed_items + 1 >= total_items
                        THEN 'COMPLETED_WITH_ERRORS'
                    ELSE 'PROCESSING'
                END,
                completed_at = CASE
                    WHEN processed_items + 1 >= total_items
                        THEN CURRENT_TIMESTAMP
                    ELSE completed_at
                END,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :jobId
              AND status NOT IN ('COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED')
            """, nativeQuery = true)
    int incrementFailedAndCompleteIfFinished(
            @Param("jobId") UUID jobId,
            @Param("errorMessage") String errorMessage
    );
}
