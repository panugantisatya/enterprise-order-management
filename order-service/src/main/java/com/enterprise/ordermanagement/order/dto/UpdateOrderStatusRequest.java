package com.enterprise.ordermanagement.order.dto;

import com.enterprise.ordermanagement.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull(message = "status is required")
        OrderStatus status

) {
}
