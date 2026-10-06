package com.enterprise.ordermanagement.order.exception;

public class PaymentRetryLimitExceededException extends RuntimeException {

    public PaymentRetryLimitExceededException(String message) {
        super(message);
    }
}
