package com.vicky.smartpay.payment.repository;

import com.vicky.smartpay.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByPaymentId(String paymentId);
    List<Payment> findBySenderId(Long senderId);
}