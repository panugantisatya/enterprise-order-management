package com.enterprise.ordermanagement.order.payment.repository;

import com.enterprise.ordermanagement.order.payment.entity.PaymentProcessingIdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentProcessingIdempotencyRecordRepository
        extends JpaRepository<PaymentProcessingIdempotencyRecord, UUID> {

    Optional<PaymentProcessingIdempotencyRecord>
    findByIdempotencyKey(String idempotencyKey);
}
