package com.enterprise.ordermanagement.order.payment.controller;

import com.enterprise.ordermanagement.order.payment.dto.CreatePaymentRequest;
import com.enterprise.ordermanagement.order.payment.dto.FailPaymentRequest;
import com.enterprise.ordermanagement.order.payment.dto.PaymentResponse;
import com.enterprise.ordermanagement.order.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        return ResponseEntity.accepted()
                .body(paymentService.createPayment(idempotencyKey, request));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable UUID paymentId) {

        return ResponseEntity.ok(
                paymentService.getPayment(paymentId));
    }

    @PostMapping("/{paymentId}/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @PathVariable UUID paymentId) {

        return ResponseEntity.accepted()
                .body(paymentService.processPayment(paymentId));
    }

    @PostMapping("/{paymentId}/fail")
    public ResponseEntity<PaymentResponse> failPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody FailPaymentRequest request) {

        return ResponseEntity.accepted()
                .body(paymentService.failPayment(paymentId, request));
    }

    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @PathVariable UUID paymentId) {

        return ResponseEntity.accepted()
                .body(paymentService.cancelPayment(paymentId));
    }
}
