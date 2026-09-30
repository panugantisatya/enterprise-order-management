package com.enterprise.ordermanagement.order.bulk;

import java.time.Instant;
import java.util.UUID;

public record BulkOrderJobCreatedEvent(
        UUID eventId,
        String eventType,
        UUID jobId,
        Instant occurredAt
) {
}
