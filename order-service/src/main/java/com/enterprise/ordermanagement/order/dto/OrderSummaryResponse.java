package com.enterprise.ordermanagement.order.dto;

import com.enterprise.ordermanagement.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String orderNumber,
        UUID customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
