package com.enterprise.ordermanagement.order.repository;

import com.enterprise.ordermanagement.order.entity.BulkOrderJobItem;
import com.enterprise.ordermanagement.order.entity.BulkOrderJobItem.BulkOrderJobItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BulkOrderJobItemRepository
        extends JpaRepository<BulkOrderJobItem, UUID> {

    Optional<BulkOrderJobItem> findByJobIdAndItemIndex(
            UUID jobId,
            Integer itemIndex
    );

    Optional<BulkOrderJobItem>
    findByJobIdAndItemIndexAndStatus(
            UUID jobId,
            Integer itemIndex,
            BulkOrderJobItemStatus status
    );
}
