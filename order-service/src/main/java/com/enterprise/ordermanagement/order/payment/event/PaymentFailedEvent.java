package com.enterprise.ordermanagement.order.payment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID paymentId,
        UUID orderId,
        BigDecimal amount,
        String currency,
        String provider,
        String failureReason
) {
}
