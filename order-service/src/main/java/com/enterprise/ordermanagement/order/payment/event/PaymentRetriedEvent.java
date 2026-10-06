package com.enterprise.ordermanagement.order.payment.event;

import java.time.Instant;
import java.util.UUID;

public record PaymentRetriedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID paymentId,
        UUID orderId,
        Integer retryCount
) {
}
