package com.enterprise.ordermanagement.order.repository;

import com.enterprise.ordermanagement.order.entity.Order;
import com.enterprise.ordermanagement.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByCustomerId(
            UUID customerId,
            Pageable pageable
    );

    Page<Order> findByStatus(
            OrderStatus status,
            Pageable pageable
    );

    Page<Order> findByCustomerIdAndStatus(
            UUID customerId,
            OrderStatus status,
            Pageable pageable
    );
}
