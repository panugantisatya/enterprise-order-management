package com.enterprise.ordermanagement.order.service;

import com.enterprise.ordermanagement.order.dto.CreateOrderItemRequest;
import com.enterprise.ordermanagement.order.dto.CreateOrderRequest;
import com.enterprise.ordermanagement.order.dto.OrderResponse;
import com.enterprise.ordermanagement.order.dto.OrderSearchCriteria;
import com.enterprise.ordermanagement.order.dto.OrderSummaryResponse;
import com.enterprise.ordermanagement.order.entity.IdempotencyRecord;
import com.enterprise.ordermanagement.order.entity.Order;
import com.enterprise.ordermanagement.order.entity.OrderItem;
import com.enterprise.ordermanagement.order.entity.OrderStatus;
import com.enterprise.ordermanagement.order.entity.OutboxEvent;
import com.enterprise.ordermanagement.order.event.OrderCreatedEvent;
import com.enterprise.ordermanagement.order.event.OrderEventFactory;
import com.enterprise.ordermanagement.order.exception.IdempotencyKeyConflictException;
import com.enterprise.ordermanagement.order.exception.ResourceNotFoundException;
import com.enterprise.ordermanagement.order.repository.IdempotencyRecordRepository;
import com.enterprise.ordermanagement.order.repository.OrderRepository;
import com.enterprise.ordermanagement.order.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final RequestHashService requestHashService;
    private final OrderEventFactory orderEventFactory;
    private final JsonMapper jsonMapper;

    @Transactional
    public OrderResponse createOrder(
            CreateOrderRequest request,
            String idempotencyKey
    ) {
        String requestHash = requestHashService.hash(request);

        IdempotencyRecord existingRecord =
                idempotencyRecordRepository
                        .findByIdempotencyKey(idempotencyKey)
                        .orElse(null);

        if (existingRecord != null) {

            if (!existingRecord.getRequestHash().equals(requestHash)) {
                throw new IdempotencyKeyConflictException(
                        "Idempotency key has already been used with a different request"
                );
            }

            Order existingOrder =
                    orderRepository.findById(existingRecord.getOrderId())
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Order associated with idempotency key was not found"
                                    )
                            );

            return toResponse(existingOrder);
        }

        Order order = new Order(
                orderNumberGenerator.generate(),
                request.customerId(),
                request.currency().toUpperCase()
        );

        for (CreateOrderItemRequest itemRequest : request.items()) {

            OrderItem item = new OrderItem(
                    itemRequest.productId(),
                    itemRequest.quantity(),
                    itemRequest.unitPrice()
            );

            order.addItem(item);
        }

        Order savedOrder = orderRepository.save(order);

        UUID eventId = UUID.randomUUID();

        OrderCreatedEvent orderCreatedEvent =
                orderEventFactory.createOrderCreatedEvent(
                        savedOrder,
                        eventId
                );

        String payload = serializeEvent(orderCreatedEvent);

        OutboxEvent outboxEvent =
                new OutboxEvent(
                        "ORDER",
                        savedOrder.getId(),
                        "OrderCreated",
                        payload
                );

        outboxEventRepository.save(outboxEvent);

        IdempotencyRecord record =
                new IdempotencyRecord(
                        idempotencyKey,
                        requestHash,
                        savedOrder.getId()
                );

        idempotencyRecordRepository.save(record);

        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> searchOrders(
            OrderSearchCriteria criteria,
            Pageable pageable
    ) {
        Page<Order> orders;

        if (criteria.customerId() != null
                && criteria.status() != null) {

            orders = orderRepository.findByCustomerIdAndStatus(
                    criteria.customerId(),
                    criteria.status(),
                    pageable
            );

        } else if (criteria.customerId() != null) {

            orders = orderRepository.findByCustomerId(
                    criteria.customerId(),
                    pageable
            );

        } else if (criteria.status() != null) {

            orders = orderRepository.findByStatus(
                    criteria.status(),
                    pageable
            );

        } else {

            orders = orderRepository.findAll(pageable);
        }

        return orders.map(this::toSummaryResponse);
    }

    @Transactional
    public OrderResponse updateOrderStatus(
            UUID orderId,
            OrderStatus newStatus
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        order.changeStatus(newStatus);

        return toResponse(order);
    }

    private String serializeEvent(OrderCreatedEvent event) {
        try {
            return jsonMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Failed to serialize order created event",
                    exception
            );
        }
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items =
                order.getItems()
                        .stream()
                        .map(item -> new OrderResponse.OrderItemResponse(
                                item.getId(),
                                item.getProductId(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getLineTotal()
                        ))
                        .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getVersion(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }

    private OrderSummaryResponse toSummaryResponse(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getVersion(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
