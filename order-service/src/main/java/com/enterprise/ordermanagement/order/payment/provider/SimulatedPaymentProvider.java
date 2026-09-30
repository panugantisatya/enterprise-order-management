package com.enterprise.ordermanagement.order.payment.provider;

import com.enterprise.ordermanagement.order.payment.entity.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SimulatedPaymentProvider implements PaymentProvider {

    private static final String PROVIDER_NAME = "SIMULATED";

    private final boolean decline;

    public SimulatedPaymentProvider(
            @Value("${payment.provider.simulated.decline:false}")
            boolean decline) {
        this.decline = decline;
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supports(String provider) {
        return provider != null
                && PROVIDER_NAME.equalsIgnoreCase(provider);
    }

    @Override
    public PaymentProviderResult process(Payment payment) {

        if (!supports(payment.getProvider())) {
            throw new IllegalArgumentException(
                    "Unsupported payment provider: " + payment.getProvider()
            );
        }

        if (decline) {
            return PaymentProviderResult.failure(
                    "M18 simulated provider decline");
        }

        return PaymentProviderResult.success(
                "SIM-" + UUID.randomUUID());
    }
}
