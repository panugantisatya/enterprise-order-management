package com.enterprise.ordermanagement.order.exception;

public class BulkOrderRetryLimitExceededException
        extends RuntimeException {

    public BulkOrderRetryLimitExceededException(String message) {
        super(message);
    }
}
