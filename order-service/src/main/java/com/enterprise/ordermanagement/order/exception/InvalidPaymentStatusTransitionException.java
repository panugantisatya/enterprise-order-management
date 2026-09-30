package com.enterprise.ordermanagement.order.exception;

public class InvalidPaymentStatusTransitionException
        extends RuntimeException {

    public InvalidPaymentStatusTransitionException(String message) {
        super(message);
    }
}
