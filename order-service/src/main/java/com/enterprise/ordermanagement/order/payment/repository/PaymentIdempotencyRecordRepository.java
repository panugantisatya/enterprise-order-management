package com.enterprise.ordermanagement.order.payment.repository;

import com.enterprise.ordermanagement.order.payment.entity.PaymentIdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentIdempotencyRecordRepository
        extends JpaRepository<PaymentIdempotencyRecord, UUID> {

    Optional<PaymentIdempotencyRecord> findByIdempotencyKey(
            String idempotencyKey
    );
}
