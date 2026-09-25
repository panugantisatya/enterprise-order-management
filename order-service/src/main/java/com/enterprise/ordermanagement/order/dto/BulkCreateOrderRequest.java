package com.enterprise.ordermanagement.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record BulkCreateOrderRequest(

        @NotEmpty(message = "bulk request must contain at least one order")
        @Size(
                max = 1000,
                message = "bulk request cannot contain more than 1000 orders"
        )
        @Valid
        List<BulkOrderItem> orders

) {

    public record BulkOrderItem(

            @NotNull(message = "customerId is required")
            UUID customerId,

            @NotNull(message = "currency is required")
            @Size(
                    min = 3,
                    max = 3,
                    message = "currency must be a 3-letter ISO currency code"
            )
            String currency,

            @NotEmpty(message = "order must contain at least one item")
            @Valid
            List<CreateOrderItemRequest> items
    ) {
    }
}
