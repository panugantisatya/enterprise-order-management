package com.enterprise.ordermanagement.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(

        @NotNull(message = "customerId is required")
        UUID customerId,

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO currency code")
        String currency,

        @NotEmpty(message = "order must contain at least one item")
        @Valid
        List<CreateOrderItemRequest> items

) {
}
