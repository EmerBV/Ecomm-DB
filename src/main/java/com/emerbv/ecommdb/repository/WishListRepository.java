package com.emerbv.ecommdb.repository;

import com.emerbv.ecommdb.model.WishList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishListRepository extends JpaRepository<WishList, Long> {
    Optional<WishList> findByUserId(Long userId);
    boolean existsByUserIdAndProductsId(Long userId, Long productId);
}
