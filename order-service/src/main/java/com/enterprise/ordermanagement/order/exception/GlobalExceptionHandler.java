package com.enterprise.ordermanagement.order.exception;

import com.enterprise.ordermanagement.order.payment.exception.InvalidPaymentStatusTransitionException;
import com.enterprise.ordermanagement.order.payment.exception.PaymentAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ErrorResponse.FieldError> errors =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error -> new ErrorResponse.FieldError(
                                error.getField(),
                                error.getDefaultMessage()
                        ))
                        .toList();

        ErrorResponse response = new ErrorResponse(
                "VALIDATION_FAILED",
                "Request validation failed",
                Instant.now(),
                request.getRequestURI(),
                errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
                "RESOURCE_NOT_FOUND",
                exception.getMessage(),
                Instant.now(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(BulkOrderRetryNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleBulkOrderRetryNotAllowedException(
            BulkOrderRetryNotAllowedException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
                "BULK_ORDER_RETRY_NOT_ALLOWED",
                exception.getMessage(),
                Instant.now(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(BulkOrderRetryLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleBulkOrderRetryLimitExceededException(
            BulkOrderRetryLimitExceededException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
                "BULK_ORDER_RETRY_LIMIT_EXCEEDED",
                exception.getMessage(),
                Instant.now(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(InvalidPaymentStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPaymentStatusTransitionException(
            InvalidPaymentStatusTransitionException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
                "INVALID_PAYMENT_STATUS_TRANSITION",
                exception.getMessage(),
                Instant.now(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(PaymentAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handlePaymentAlreadyExistsException(
            PaymentAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
                "PAYMENT_ALREADY_EXISTS",
                exception.getMessage(),
                Instant.now(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(InvalidOrderStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderStatusTransitionException(
            InvalidOrderStatusTransitionException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
                "INVALID_ORDER_STATUS_TRANSITION",
                exception.getMessage(),
                Instant.now(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(IdempotencyKeyRequiredException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyKeyRequiredException(
            IdempotencyKeyRequiredException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
                "IDEMPOTENCY_KEY_REQUIRED",
                exception.getMessage(),
                Instant.now(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(IdempotencyKeyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyKeyConflictException(
            IdempotencyKeyConflictException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
                "IDEMPOTENCY_KEY_CONFLICT",
                exception.getMessage(),
                Instant.now(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
                "INVALID_REQUEST",
                exception.getMessage(),
                Instant.now(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                Instant.now(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
