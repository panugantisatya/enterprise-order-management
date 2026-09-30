package com.enterprise.ordermanagement.order.exception;

public class PaymentAlreadyExistsException
        extends RuntimeException {

    public PaymentAlreadyExistsException(String message) {
        super(message);
    }
}
