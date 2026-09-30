package com.enterprise.ordermanagement.order.payment.dto;

import com.enterprise.ordermanagement.order.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String provider,
        String providerPaymentId,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt,
        Long version
) {

    public static PaymentResponse from(
            com.enterprise.ordermanagement.order.payment.entity.Payment payment
    ) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getProvider(),
                payment.getProviderPaymentId(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getCompletedAt(),
                payment.getVersion()
        );
    }
}
