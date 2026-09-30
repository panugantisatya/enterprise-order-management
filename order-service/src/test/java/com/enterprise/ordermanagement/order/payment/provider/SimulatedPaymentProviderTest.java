package com.enterprise.ordermanagement.order.payment.provider;

import com.enterprise.ordermanagement.order.payment.entity.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SimulatedPaymentProviderTest {

    @Test
    void shouldSupportSimulatedProvider() {

        SimulatedPaymentProvider provider =
                new SimulatedPaymentProvider(false);

        assertTrue(provider.supports("SIMULATED"));
        assertTrue(provider.supports("simulated"));
        assertFalse(provider.supports("STRIPE"));
        assertFalse(provider.supports(null));
    }

    @Test
    void shouldReturnSuccessfulResult() {

        SimulatedPaymentProvider provider =
                new SimulatedPaymentProvider(false);

        Payment payment = new Payment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("200.00"),
                "INR",
                "SIMULATED");

        PaymentProviderResult result =
                provider.process(payment);

        assertNotNull(result);
        assertTrue(result.successful());
        assertNotNull(result.providerPaymentId());
        assertTrue(result.providerPaymentId().startsWith("SIM-"));
        assertNull(result.failureReason());
    }

    @Test
    void shouldReturnFailureResultWhenConfiguredToDecline() {

        SimulatedPaymentProvider provider =
                new SimulatedPaymentProvider(true);

        Payment payment = new Payment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("200.00"),
                "INR",
                "SIMULATED");

        PaymentProviderResult result =
                provider.process(payment);

        assertNotNull(result);
        assertFalse(result.successful());
        assertNull(result.providerPaymentId());
        assertEquals(
                "M18 simulated provider decline",
                result.failureReason());
    }

    @Test
    void shouldRejectUnsupportedProvider() {

        SimulatedPaymentProvider provider =
                new SimulatedPaymentProvider(false);

        Payment payment = new Payment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("200.00"),
                "INR",
                "STRIPE");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> provider.process(payment));

        assertEquals(
                "Unsupported payment provider: STRIPE",
                exception.getMessage());
    }
}
