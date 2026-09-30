package com.enterprise.ordermanagement.order.payment.provider;

import com.enterprise.ordermanagement.order.payment.entity.Payment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SimulatedPaymentProvider implements PaymentProvider {

    private static final String PROVIDER_NAME = "SIMULATED";

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
    public String process(Payment payment) {

        if (!supports(payment.getProvider())) {
            throw new IllegalArgumentException(
                    "Unsupported payment provider: "
                            + payment.getProvider());
        }

        return "SIM-" + UUID.randomUUID();
    }
}
