package com.enterprise.ordermanagement.order.controller;

import com.enterprise.ordermanagement.order.dto.CreateOrderRequest;
import com.enterprise.ordermanagement.order.dto.OrderResponse;
import com.enterprise.ordermanagement.order.dto.OrderSearchCriteria;
import com.enterprise.ordermanagement.order.dto.OrderSummaryResponse;
import com.enterprise.ordermanagement.order.dto.UpdateOrderStatusRequest;
import com.enterprise.ordermanagement.order.entity.OrderStatus;
import com.enterprise.ordermanagement.order.exception.IdempotencyKeyRequiredException;
import com.enterprise.ordermanagement.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader(
                    value = "Idempotency-Key",
                    required = false
            )
            String idempotencyKey,

            @Valid @RequestBody CreateOrderRequest request
    ) {
        if (idempotencyKey == null
                || idempotencyKey.isBlank()) {

            throw new IdempotencyKeyRequiredException(
                    "Idempotency-Key header is required"
            );
        }

        OrderResponse response =
                orderService.createOrder(
                        request,
                        idempotencyKey
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID orderId
    ) {
        OrderResponse response =
                orderService.getOrder(orderId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> searchOrders(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        OrderSearchCriteria criteria =
                new OrderSearchCriteria(
                        customerId,
                        status
                );

        Page<OrderSummaryResponse> response =
                orderService.searchOrders(
                        criteria,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        OrderResponse response =
                orderService.updateOrderStatus(
                        orderId,
                        request.status()
                );

        return ResponseEntity.ok(response);
    }
}
