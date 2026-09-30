package com.enterprise.ordermanagement.order.payment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentSucceededEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID paymentId,
        UUID orderId,
        BigDecimal amount,
        String currency,
        String provider,
        String providerPaymentId
) {
}
