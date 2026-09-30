package com.enterprise.ordermanagement.order.payment.exception;

public class InvalidPaymentStatusTransitionException extends RuntimeException {

    public InvalidPaymentStatusTransitionException(String message) {
        super(message);
    }
}
