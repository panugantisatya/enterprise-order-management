package com.enterprise.ordermanagement.order.repository;

import com.enterprise.ordermanagement.order.entity.BulkOrderJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BulkOrderJobRepository
        extends JpaRepository<BulkOrderJob, UUID> {

    Optional<BulkOrderJob> findByIdempotencyKey(String idempotencyKey);
}
