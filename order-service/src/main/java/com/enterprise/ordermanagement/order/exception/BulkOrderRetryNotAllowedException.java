package com.enterprise.ordermanagement.order.exception;

public class BulkOrderRetryNotAllowedException
        extends RuntimeException {

    public BulkOrderRetryNotAllowedException(String message) {
        super(message);
    }
}
