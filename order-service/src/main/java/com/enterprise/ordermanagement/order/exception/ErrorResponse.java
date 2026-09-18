package com.enterprise.ordermanagement.order.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String path,
        List<FieldError> errors
) {

    public record FieldError(
            String field,
            String message
    ) {
    }
}
