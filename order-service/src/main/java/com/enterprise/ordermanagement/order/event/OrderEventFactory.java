package com.enterprise.ordermanagement.order.event;

import com.enterprise.ordermanagement.order.entity.Order;
import com.enterprise.ordermanagement.order.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OrderEventFactory {

    public OrderCreatedEvent createOrderCreatedEvent(
            Order order,
            UUID eventId
    ) {
        List<OrderCreatedEvent.OrderCreatedItem> items =
                order.getItems()
                        .stream()
                        .map(this::toEventItem)
                        .toList();

        return new OrderCreatedEvent(
                eventId,
                "OrderCreated",
                Instant.now(),
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getCurrency(),
                items
        );
    }

    private OrderCreatedEvent.OrderCreatedItem toEventItem(
            OrderItem item
    ) {
        return new OrderCreatedEvent.OrderCreatedItem(
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal()
        );
    }
}
