package com.enterprise.ordermanagement.order.bulk;

import com.enterprise.ordermanagement.order.dto.BulkCreateOrderRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BulkOrderJobCreatedEvent(
        UUID eventId,
        String eventType,
        UUID jobId,
        Instant occurredAt,
        List<BulkCreateOrderRequest.BulkOrderItem> orders
) {
}
