package com.enterprise.ordermanagement.order.payment.repository;

import com.enterprise.ordermanagement.order.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :paymentId")
    Optional<Payment> findByIdForUpdate(
            @Param("paymentId") UUID paymentId
    );

    @Modifying
    @Query("""
            update Payment p
               set p.status = com.enterprise.ordermanagement.order.payment.entity.PaymentStatus.PENDING,
                   p.retryCount = p.retryCount + 1,
                   p.failureReason = null,
                   p.providerPaymentId = null,
                   p.completedAt = null
             where p.id = :paymentId
               and p.status = com.enterprise.ordermanagement.order.payment.entity.PaymentStatus.FAILED
               and p.retryCount < :maxRetries
            """)
    int retryFailedPayment(
            @Param("paymentId") UUID paymentId,
            @Param("maxRetries") int maxRetries
    );
}
