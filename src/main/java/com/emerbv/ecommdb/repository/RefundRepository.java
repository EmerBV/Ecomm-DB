package com.emerbv.ecommdb.repository;

import com.emerbv.ecommdb.model.Order;
import com.emerbv.ecommdb.model.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByStripeRefundId(String stripeRefundId);

    List<Refund> findByOrder(Order order);

    List<Refund> findByOrderOrderId(Long orderId);

    @Query("SELECT SUM(r.amount) FROM Refund r WHERE r.order.orderId = :orderId AND r.status = 'SUCCEEDED'")
    BigDecimal getTotalRefundedAmountByOrderId(@Param("orderId") Long orderId);
}
