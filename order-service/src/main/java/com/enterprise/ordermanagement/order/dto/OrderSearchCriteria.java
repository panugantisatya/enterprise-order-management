package com.enterprise.ordermanagement.order.dto;

import com.enterprise.ordermanagement.order.entity.OrderStatus;

import java.util.UUID;

public record OrderSearchCriteria(
        UUID customerId,
        OrderStatus status
) {
}
