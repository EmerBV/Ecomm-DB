package com.emerbv.ecommdb.repository;

import com.emerbv.ecommdb.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByOrderOrderId(Long orderId);
    Optional<PaymentTransaction> findByPaymentIntentId(String paymentIntentId);
}
