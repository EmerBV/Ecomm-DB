package com.emerbv.ecommdb.repository;

import com.emerbv.ecommdb.model.ShippingDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShippingDetailsRepository extends JpaRepository<ShippingDetails, Long> {
    List<ShippingDetails> findByUserId(Long userId);
    Optional<ShippingDetails> findByUserIdAndIsDefaultTrue(Long userId);
}
