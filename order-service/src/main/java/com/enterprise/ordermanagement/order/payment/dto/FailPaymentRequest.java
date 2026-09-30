package com.enterprise.ordermanagement.order.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record FailPaymentRequest(

        @NotBlank(message = "failureReason is required")
        String failureReason

) {
}
