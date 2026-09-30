package com.enterprise.ordermanagement.order.payment.provider;

import com.enterprise.ordermanagement.order.payment.entity.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SimulatedPaymentProviderTest {

    private final SimulatedPaymentProvider provider =
            new SimulatedPaymentProvider();

    @Test
    void shouldSupportSimulatedProvider() {

        assertEquals(
                "SIMULATED",
                provider.providerName());

        assertTrue(
                provider.supports("SIMULATED"));

        assertTrue(
                provider.supports("simulated"));
    }

    @Test
    void shouldRejectUnsupportedProvider() {

        assertFalse(
                provider.supports("RAZORPAY"));

        assertFalse(
                provider.supports("STRIPE"));

        assertFalse(
                provider.supports(null));
    }

    @Test
    void shouldGenerateSimulatedProviderPaymentId() {

        Payment payment =
                new Payment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("100.00"),
                        "INR",
                        "SIMULATED");

        String providerPaymentId =
                provider.process(payment);

        assertNotNull(providerPaymentId);
        assertTrue(
                providerPaymentId.startsWith("SIM-"));
    }

    @Test
    void shouldRejectPaymentForUnsupportedProvider() {

        Payment payment =
                new Payment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("100.00"),
                        "INR",
                        "RAZORPAY");

        assertThrows(
                IllegalArgumentException.class,
                () -> provider.process(payment));
    }
}
