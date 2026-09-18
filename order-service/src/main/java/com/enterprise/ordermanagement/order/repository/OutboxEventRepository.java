package com.enterprise.ordermanagement.order.repository;

import com.enterprise.ordermanagement.order.entity.OutboxEvent;
import com.enterprise.ordermanagement.order.entity.OutboxEvent.OutboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("""
            SELECT e
            FROM OutboxEvent e
            WHERE e.status = :status
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(
            OutboxEventStatus status,
            Pageable pageable
    );
}
