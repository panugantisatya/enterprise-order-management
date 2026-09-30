package com.enterprise.ordermanagement.order.payment.service;

import com.enterprise.ordermanagement.order.entity.Order;
import com.enterprise.ordermanagement.order.entity.OutboxEvent;
import com.enterprise.ordermanagement.order.payment.dto.CreatePaymentRequest;
import com.enterprise.ordermanagement.order.payment.dto.FailPaymentRequest;
import com.enterprise.ordermanagement.order.payment.dto.PaymentResponse;
import com.enterprise.ordermanagement.order.payment.entity.Payment;
import com.enterprise.ordermanagement.order.payment.entity.PaymentIdempotencyRecord;
import com.enterprise.ordermanagement.order.payment.event.PaymentCancelledEvent;
import com.enterprise.ordermanagement.order.payment.event.PaymentCreatedEvent;
import com.enterprise.ordermanagement.order.payment.event.PaymentFailedEvent;
import com.enterprise.ordermanagement.order.payment.event.PaymentSucceededEvent;
import com.enterprise.ordermanagement.order.payment.exception.PaymentAlreadyExistsException;
import com.enterprise.ordermanagement.order.payment.exception.PaymentProviderNotSupportedException;
import com.enterprise.ordermanagement.order.payment.provider.PaymentProvider;
import com.enterprise.ordermanagement.order.payment.provider.PaymentProviderResult;
import com.enterprise.ordermanagement.order.payment.repository.PaymentIdempotencyRecordRepository;
import com.enterprise.ordermanagement.order.payment.repository.PaymentRepository;
import com.enterprise.ordermanagement.order.repository.OrderRepository;
import com.enterprise.ordermanagement.order.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentIdempotencyRecordRepository idempotencyRepository;
    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;
    private final PaymentProvider paymentProvider;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentIdempotencyRecordRepository idempotencyRepository,
            OrderRepository orderRepository,
            OutboxEventRepository outboxEventRepository,
            JsonMapper jsonMapper,
            PaymentProvider paymentProvider) {

        this.paymentRepository = paymentRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.jsonMapper = jsonMapper;
        this.paymentProvider = paymentProvider;
    }

    @Transactional
    public PaymentResponse createPayment(
            String idempotencyKey,
            CreatePaymentRequest request) {

        String requestHash = hashPaymentRequest(request);

        var existingIdempotency =
                idempotencyRepository.findByIdempotencyKey(idempotencyKey);

        if (existingIdempotency.isPresent()) {

            PaymentIdempotencyRecord record = existingIdempotency.get();

            if (!record.getRequestHash().equals(requestHash)) {
                throw new IllegalArgumentException(
                        "Idempotency-Key was already used with a different request");
            }

            Payment existingPayment = paymentRepository
                    .findById(record.getPaymentId())
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Payment referenced by idempotency record was not found"));

            return PaymentResponse.from(existingPayment);
        }

        if (!paymentProvider.supports(request.provider())) {
            throw new PaymentProviderNotSupportedException(
                    "Unsupported payment provider: "
                            + request.provider()
                            + ". Supported provider: "
                            + paymentProvider.providerName());
        }

        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Order not found: " + request.orderId()));

        if (order.getTotalAmount() == null
                || order.getTotalAmount().compareTo(request.amount()) != 0) {

            throw new IllegalArgumentException(
                    "Payment amount must match order total amount");
        }

        if (order.getCurrency() == null
                || !order.getCurrency().equalsIgnoreCase(request.currency())) {

            throw new IllegalArgumentException(
                    "Payment currency must match order currency");
        }

        if (paymentRepository.findByOrderId(request.orderId()).isPresent()) {
            throw new PaymentAlreadyExistsException(
                    "Payment already exists for order: " + request.orderId());
        }

        String providerName =
                request.provider().toUpperCase();

        Payment payment = new Payment(
                request.orderId(),
                order.getCustomerId(),
                request.amount(),
                request.currency().toUpperCase(),
                providerName);

        paymentRepository.save(payment);

        PaymentCreatedEvent event = new PaymentCreatedEvent(
                UUID.randomUUID(),
                "PaymentCreated",
                Instant.now(),
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getProvider());

        saveOutboxEvent(
                payment.getId(),
                "PaymentCreated",
                event);

        PaymentIdempotencyRecord idempotencyRecord =
                new PaymentIdempotencyRecord(
                        idempotencyKey,
                        requestHash,
                        payment.getId());

        idempotencyRepository.save(idempotencyRecord);

        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payment not found: " + paymentId));

        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse processPayment(UUID paymentId) {

        Payment payment = findPayment(paymentId);

        if (!paymentProvider.supports(payment.getProvider())) {
            throw new PaymentProviderNotSupportedException(
                    "No payment provider implementation available for: "
                            + payment.getProvider());
        }

        payment.startProcessing();

        PaymentProviderResult providerResult =
                paymentProvider.process(payment);

        if (providerResult.successful()) {

            payment.markSucceeded(
                    providerResult.providerPaymentId());

            PaymentSucceededEvent event = new PaymentSucceededEvent(
                    UUID.randomUUID(),
                    "PaymentSucceeded",
                    Instant.now(),
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getAmount(),
                    payment.getCurrency(),
                    payment.getProvider(),
                    payment.getProviderPaymentId());

            saveOutboxEvent(
                    payment.getId(),
                    "PaymentSucceeded",
                    event);

        } else {

            payment.markFailed(
                    providerResult.failureReason());

            PaymentFailedEvent event = new PaymentFailedEvent(
                    UUID.randomUUID(),
                    "PaymentFailed",
                    Instant.now(),
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getAmount(),
                    payment.getCurrency(),
                    payment.getProvider(),
                    payment.getFailureReason());

            saveOutboxEvent(
                    payment.getId(),
                    "PaymentFailed",
                    event);
        }

        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse failPayment(
            UUID paymentId,
            FailPaymentRequest request) {

        Payment payment = findPayment(paymentId);

        payment.startProcessing();

        payment.markFailed(request.failureReason());

        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(),
                "PaymentFailed",
                Instant.now(),
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getProvider(),
                payment.getFailureReason());

        saveOutboxEvent(
                payment.getId(),
                "PaymentFailed",
                event);

        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse cancelPayment(UUID paymentId) {

        Payment payment = findPayment(paymentId);

        payment.cancel();

        PaymentCancelledEvent event = new PaymentCancelledEvent(
                UUID.randomUUID(),
                "PaymentCancelled",
                Instant.now(),
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getProvider());

        saveOutboxEvent(
                payment.getId(),
                "PaymentCancelled",
                event);

        return PaymentResponse.from(payment);
    }

    private Payment findPayment(UUID paymentId) {

        return paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payment not found: " + paymentId));
    }

    private void saveOutboxEvent(
            UUID paymentId,
            String eventType,
            Object event) {

        try {

            String payload =
                    jsonMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = new OutboxEvent(
                    "PAYMENT",
                    paymentId,
                    eventType,
                    payload);

            outboxEventRepository.save(outboxEvent);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to serialize payment event",
                    e);
        }
    }

    private String hashPaymentRequest(
            CreatePaymentRequest request) {

        String canonical =
                request.orderId()
                        + "|"
                        + request.amount()
                        + "|"
                        + request.currency().toUpperCase()
                        + "|"
                        + request.provider().toUpperCase();

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            canonical.getBytes(
                                    StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to hash payment request",
                    e);
        }
    }
}
