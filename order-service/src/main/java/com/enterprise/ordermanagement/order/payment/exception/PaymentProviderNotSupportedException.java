package com.enterprise.ordermanagement.order.payment.exception;

public class PaymentProviderNotSupportedException
        extends IllegalArgumentException {

    public PaymentProviderNotSupportedException(String message) {
        super(message);
    }
}
