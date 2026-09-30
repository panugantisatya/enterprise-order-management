package com.enterprise.ordermanagement.order.payment.provider;

import com.enterprise.ordermanagement.order.payment.entity.Payment;

public interface PaymentProvider {

    String providerName();

    boolean supports(String provider);

    String process(Payment payment);
}
