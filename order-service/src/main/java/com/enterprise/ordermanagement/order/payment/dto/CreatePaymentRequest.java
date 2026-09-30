package com.enterprise.ordermanagement.order.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(

        @NotNull(message = "orderId is required")
        UUID orderId,

        @NotNull(message = "amount is required")
        @DecimalMin(
                value = "0.01",
                message = "amount must be greater than zero"
        )
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        @Size(
                min = 3,
                max = 3,
                message = "currency must be a 3-letter ISO currency code"
        )
        String currency,

        @NotBlank(message = "provider is required")
        String provider

) {
}
