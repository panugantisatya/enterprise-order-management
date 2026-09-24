package com.enterprise.ordermanagement.order.repository;

import com.enterprise.ordermanagement.order.entity.OutboxEvent;
import com.enterprise.ordermanagement.order.entity.OutboxEvent.OutboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    @Query("""
            SELECT e
            FROM OutboxEvent e
            WHERE e.status = :status
              AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= :now)
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findReadyEvents(
            OutboxEventStatus status,
            Instant now,
            Pageable pageable
    );

    @Modifying
    @Query("""
            UPDATE OutboxEvent e
            SET e.status = :processingStatus
            WHERE e.id IN :eventIds
              AND e.status = :pendingStatus
            """)
    int claimEvents(
            List<UUID> eventIds,
            OutboxEventStatus pendingStatus,
            OutboxEventStatus processingStatus
    );
}
