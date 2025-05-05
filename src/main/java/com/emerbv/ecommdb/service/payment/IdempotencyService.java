package com.emerbv.ecommdb.service.payment;

import com.emerbv.ecommdb.model.IdempotencyRecord;
import com.emerbv.ecommdb.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;

    /**
     * Genera un nuevo token de idempotencia
     * @return Token UUID generado
     */
    public String generateIdempotencyKey() {
        return UUID.randomUUID().toString();
    }

    /**
     * Verifica si una operación con la clave de idempotencia dada ya se ha procesado
     * @param key Clave de idempotencia
     * @param operationType Tipo de operación (PAYMENT, REFUND, etc.)
     * @return Optional con el registro si existe, empty si no
     */
    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> findByKey(String key, String operationType) {
        return idempotencyRepository.findByKeyAndOperationType(key, operationType);
    }

    /**
     * Registra una nueva operación con clave de idempotencia
     * @param key Clave de idempotencia
     * @param operationType Tipo de operación
     * @param entityId ID de la entidad relacionada (orden, pago, etc.)
     * @param status Estado de la operación
     * @return El registro creado
     */
    @Transactional
    public IdempotencyRecord recordOperation(String key, String operationType, String entityId, String status) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setKey(key);
        record.setOperationType(operationType);
        record.setEntityId(entityId);
        record.setStatus(status);
        record.setCreatedAt(LocalDateTime.now());

        return idempotencyRepository.save(record);
    }

    /**
     * Actualiza el estado de una operación existente
     */
    @Transactional
    public IdempotencyRecord updateOperationStatus(String key, String operationType, String status) {
        IdempotencyRecord record = idempotencyRepository.findByKeyAndOperationType(key, operationType)
                .orElseThrow(() -> new IllegalStateException("No se encontró el registro de idempotencia"));

        record.setStatus(status);
        record.setUpdatedAt(LocalDateTime.now());

        return idempotencyRepository.save(record);
    }

    /**
     * Elimina registros antiguos para mantener la tabla limpia
     */
    @Transactional
    public void cleanupOldRecords() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30); // Conservar 30 días
        idempotencyRepository.deleteByCreatedAtBefore(cutoffDate);
    }
}
