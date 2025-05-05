package com.emerbv.ecommdb.repository;

import com.emerbv.ecommdb.model.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    Optional<Dispute> findByStripeDisputeId(String stripeDisputeId);

    List<Dispute> findByOrderOrderId(Long orderId);

    List<Dispute> findByStatus(String status);
}
