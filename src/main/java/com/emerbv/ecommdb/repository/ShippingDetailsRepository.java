package com.emerbv.ecommdb.repository;

import com.emerbv.ecommdb.model.ShippingDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingDetailsRepository extends JpaRepository<ShippingDetails, Long> {
    ShippingDetails findByUserId(Long userId);
}
