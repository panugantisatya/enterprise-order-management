package com.enterprise.ordermanagement.order.payment.repository;

import com.enterprise.ordermanagement.order.payment.entity.PaymentRetryIdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRetryIdempotencyRecordRepository
        extends JpaRepository<PaymentRetryIdempotencyRecord, UUID> {

    Optional<PaymentRetryIdempotencyRecord>
    findByIdempotencyKey(String idempotencyKey);
}
