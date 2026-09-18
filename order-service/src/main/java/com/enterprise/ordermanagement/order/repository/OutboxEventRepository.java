package com.enterprise.ordermanagement.order.repository;

import com.enterprise.ordermanagement.order.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {
}
