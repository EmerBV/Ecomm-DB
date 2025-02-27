package com.emerbv.ecommdb.repository;

import com.emerbv.ecommdb.model.Image;
import com.emerbv.ecommdb.model.Variant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VariantRepository extends JpaRepository<Variant, Long> {
    List<Variant> findByProductId(Long id);
}
