package com.enterprise.ordermanagement.order.payment.entity;

import com.enterprise.ordermanagement.order.exception.InvalidPaymentStatusTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    @Test
    void shouldMoveFromPendingToProcessingToSucceeded() {

        Payment payment =
                new Payment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("300.00"),
                        "INR",
                        "SIMULATED"
                );

        assertEquals(
                PaymentStatus.PENDING,
                payment.getStatus()
        );

        payment.startProcessing();

        assertEquals(
                PaymentStatus.PROCESSING,
                payment.getStatus()
        );

        payment.markSucceeded("SIM-123");

        assertEquals(
                PaymentStatus.SUCCEEDED,
                payment.getStatus()
        );

        assertEquals(
                "SIM-123",
                payment.getProviderPaymentId()
        );

        assertNotNull(payment.getCompletedAt());
    }

    @Test
    void shouldMoveFromProcessingToFailed() {

        Payment payment =
                new Payment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("300.00"),
                        "INR",
                        "SIMULATED"
                );

        payment.startProcessing();
        payment.markFailed("Provider declined");

        assertEquals(
                PaymentStatus.FAILED,
                payment.getStatus()
        );

        assertEquals(
                "Provider declined",
                payment.getFailureReason()
        );

        assertNotNull(payment.getCompletedAt());
    }

    @Test
    void shouldCancelPendingPayment() {

        Payment payment =
                new Payment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("300.00"),
                        "INR",
                        "SIMULATED"
                );

        payment.cancel();

        assertEquals(
                PaymentStatus.CANCELLED,
                payment.getStatus()
        );

        assertNotNull(payment.getCompletedAt());
    }

    @Test
    void shouldRejectInvalidTransitionAfterSuccess() {

        Payment payment =
                new Payment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("300.00"),
                        "INR",
                        "SIMULATED"
                );

        payment.startProcessing();
        payment.markSucceeded("SIM-123");

        assertThrows(
                InvalidPaymentStatusTransitionException.class,
                () -> payment.startProcessing()
        );
    }

    @Test
    void shouldRejectCancelAfterProcessing() {

        Payment payment =
                new Payment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("300.00"),
                        "INR",
                        "SIMULATED"
                );

        payment.startProcessing();

        assertThrows(
                InvalidPaymentStatusTransitionException.class,
                payment::cancel
        );
    }
}
