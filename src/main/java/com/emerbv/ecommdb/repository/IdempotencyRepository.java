package com.emerbv.ecommdb.repository;

import com.emerbv.ecommdb.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByKeyAndOperationType(String key, String operationType);

    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);

    List<IdempotencyRecord> findByStatusAndCreatedAtAfter(String status, LocalDateTime cutoffTime);
}
