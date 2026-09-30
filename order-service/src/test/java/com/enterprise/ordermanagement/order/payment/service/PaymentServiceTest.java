package com.enterprise.ordermanagement.order.payment.service;

import com.enterprise.ordermanagement.order.entity.Order;
import com.enterprise.ordermanagement.order.entity.OutboxEvent;
import com.enterprise.ordermanagement.order.payment.dto.CreatePaymentRequest;
import com.enterprise.ordermanagement.order.payment.dto.FailPaymentRequest;
import com.enterprise.ordermanagement.order.payment.entity.Payment;
import com.enterprise.ordermanagement.order.payment.entity.PaymentIdempotencyRecord;
import com.enterprise.ordermanagement.order.payment.entity.PaymentStatus;
import com.enterprise.ordermanagement.order.payment.repository.PaymentIdempotencyRecordRepository;
import com.enterprise.ordermanagement.order.payment.repository.PaymentRepository;
import com.enterprise.ordermanagement.order.repository.OrderRepository;
import com.enterprise.ordermanagement.order.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private PaymentIdempotencyRecordRepository idempotencyRepository;
    private OrderRepository orderRepository;
    private OutboxEventRepository outboxEventRepository;

    private PaymentService paymentService;

    private UUID orderId;
    private UUID customerId;

    @BeforeEach
    void setUp() {

        paymentRepository = mock(PaymentRepository.class);
        idempotencyRepository = mock(PaymentIdempotencyRecordRepository.class);
        orderRepository = mock(OrderRepository.class);
        outboxEventRepository = mock(OutboxEventRepository.class);

        paymentService = new PaymentService(
                paymentRepository,
                idempotencyRepository,
                orderRepository,
                outboxEventRepository,
                new tools.jackson.databind.json.JsonMapper());

        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
    }

    @Test
    void shouldCreatePaymentAndPublishCreatedEvent() {

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        orderId,
                        new BigDecimal("100.00"),
                        "INR",
                        "SIMULATED");

        Order order = mock(Order.class);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(order.getTotalAmount())
                .thenReturn(new BigDecimal("100.00"));

        when(order.getCurrency())
                .thenReturn("INR");

        when(order.getCustomerId())
                .thenReturn(customerId);

        when(idempotencyRepository.findByIdempotencyKey("payment-test-1"))
                .thenReturn(Optional.empty());

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createPayment(
                "payment-test-1",
                request);

        assertNotNull(response.paymentId());
        assertEquals(orderId, response.orderId());
        assertEquals(customerId, response.customerId());
        assertEquals(new BigDecimal("100.00"), response.amount());
        assertEquals("INR", response.currency());
        assertEquals("SIMULATED", response.provider());
        assertEquals(PaymentStatus.PENDING, response.status());

        verify(paymentRepository).save(any(Payment.class));
        verify(idempotencyRepository).save(any(PaymentIdempotencyRecord.class));
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void shouldReturnExistingPaymentForSameIdempotencyKey() {

        Payment existingPayment =
                new Payment(
                        orderId,
                        customerId,
                        new BigDecimal("100.00"),
                        "INR",
                        "SIMULATED");

        UUID paymentId = existingPayment.getId();

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        orderId,
                        new BigDecimal("100.00"),
                        "INR",
                        "SIMULATED");

        PaymentIdempotencyRecord record =
                new PaymentIdempotencyRecord(
                        "payment-test-2",
                        hash(request),
                        paymentId);

        when(idempotencyRepository.findByIdempotencyKey("payment-test-2"))
                .thenReturn(Optional.of(record));

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(existingPayment));

        var response = paymentService.createPayment(
                "payment-test-2",
                request);

        assertEquals(paymentId, response.paymentId());
        assertEquals(orderId, response.orderId());
        assertEquals(PaymentStatus.PENDING, response.status());

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verify(outboxEventRepository, never())
                .save(any(OutboxEvent.class));
    }

    @Test
    void shouldRejectSameIdempotencyKeyWithDifferentRequest() {

        PaymentIdempotencyRecord record =
                new PaymentIdempotencyRecord(
                        "payment-test-3",
                        hash(new CreatePaymentRequest(
                                orderId,
                                new BigDecimal("100.00"),
                                "INR",
                                "SIMULATED")),
                        UUID.randomUUID());

        when(idempotencyRepository.findByIdempotencyKey("payment-test-3"))
                .thenReturn(Optional.of(record));

        CreatePaymentRequest differentRequest =
                new CreatePaymentRequest(
                        orderId,
                        new BigDecimal("200.00"),
                        "INR",
                        "SIMULATED");

        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createPayment(
                        "payment-test-3",
                        differentRequest));

        verify(paymentRepository, never())
                .save(any(Payment.class));
    }

    @Test
    void shouldRejectPaymentWhenPaymentAlreadyExists() {

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        orderId,
                        new BigDecimal("100.00"),
                        "INR",
                        "SIMULATED");

        Order order = mock(Order.class);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(order.getTotalAmount())
                .thenReturn(new BigDecimal("100.00"));

        when(order.getCurrency())
                .thenReturn("INR");

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(mock(Payment.class)));

        assertThrows(
                com.enterprise.ordermanagement.order.payment.exception.PaymentAlreadyExistsException.class,
                () -> paymentService.createPayment(
                        "payment-test-4",
                        request));
    }

    @Test
    void shouldProcessPaymentSuccessfully() {

        Payment payment =
                new Payment(
                        orderId,
                        customerId,
                        new BigDecimal("100.00"),
                        "INR",
                        "SIMULATED");

        UUID paymentId = payment.getId();

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(payment));

        when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response =
                paymentService.processPayment(paymentId);

        assertEquals(PaymentStatus.SUCCEEDED, response.status());
        assertNotNull(response.providerPaymentId());
        assertEquals(orderId, response.orderId());

        verify(outboxEventRepository)
                .save(any(OutboxEvent.class));
    }

    @Test
    void shouldFailPayment() {

        Payment payment =
                new Payment(
                        orderId,
                        customerId,
                        new BigDecimal("100.00"),
                        "INR",
                        "SIMULATED");

        UUID paymentId = payment.getId();

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(payment));

        when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FailPaymentRequest request =
                new FailPaymentRequest("Insufficient funds");

        var response =
                paymentService.failPayment(
                        paymentId,
                        request);

        assertEquals(PaymentStatus.FAILED, response.status());
        assertEquals(
                "Insufficient funds",
                response.failureReason());

        verify(outboxEventRepository)
                .save(any(OutboxEvent.class));
    }

    @Test
    void shouldCancelPendingPayment() {

        Payment payment =
                new Payment(
                        orderId,
                        customerId,
                        new BigDecimal("100.00"),
                        "INR",
                        "SIMULATED");

        UUID paymentId = payment.getId();

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(payment));

        when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response =
                paymentService.cancelPayment(paymentId);

        assertEquals(PaymentStatus.CANCELLED, response.status());
        assertEquals(orderId, response.orderId());

        verify(outboxEventRepository)
                .save(any(OutboxEvent.class));
    }

    private String hash(CreatePaymentRequest request) {

        String canonical =
                request.orderId()
                        + "|"
                        + request.amount()
                        + "|"
                        + request.currency().toUpperCase()
                        + "|"
                        + request.provider();

        try {

            var digest =
                    java.security.MessageDigest.getInstance("SHA-256");

            byte[] bytes =
                    digest.digest(
                            canonical.getBytes(
                                    java.nio.charset.StandardCharsets.UTF_8));

            return java.util.HexFormat.of().formatHex(bytes);

        } catch (Exception e) {

            throw new IllegalStateException(e);
        }
    }
}
