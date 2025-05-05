package com.emerbv.ecommdb.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "idempotency_records",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"key", "operation_type"})})
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 36)
    private String key; // UUID como string

    @Column(name = "operation_type", nullable = false)
    private String operationType; // PAYMENT_INTENT, PAYMENT_CONFIRM, REFUND, etc.

    @Column(name = "entity_id", nullable = false)
    private String entityId; // ID de la entidad relacionada (paymentIntentId, etc.)

    @Column(name = "status", nullable = false)
    private String status; // SUCCESS, ERROR, PENDING, etc.

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Opcionalmente almacenar los detalles del error si ocurrió alguno
    @Column(columnDefinition = "TEXT")
    private String errorDetails;
}
