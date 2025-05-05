package com.emerbv.ecommdb.repository;

import com.emerbv.ecommdb.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para operaciones con notificaciones
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Busca notificaciones enviadas a un usuario específico
     */
    List<Notification> findByUserIdOrderBySentAtDesc(Long userId);

    /**
     * Busca notificaciones por tipo
     */
    Page<Notification> findByType(String type, Pageable pageable);

    /**
     * Busca notificaciones fallidas que puedan requerir reintento
     */
    @Query("SELECT n FROM Notification n WHERE n.success = false AND n.retryCount < :maxRetries AND n.sentAt > :cutoffDate")
    List<Notification> findFailedNotificationsForRetry(@Param("maxRetries") int maxRetries, @Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Encuentra notificaciones relacionadas con una entidad específica (por ejemplo, un pedido)
     */
    List<Notification> findByRelatedEntityTypeAndRelatedEntityId(String entityType, Long entityId);

    /**
     * Cuenta notificaciones por tipo y estado de éxito en un período
     */
    @Query("SELECT n.type, n.success, COUNT(n) FROM Notification n WHERE n.sentAt BETWEEN :startDate AND :endDate GROUP BY n.type, n.success")
    List<Object[]> countByTypeAndSuccessInPeriod(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Busca notificaciones por canal y período
     */
    Page<Notification> findByChannelAndSentAtBetween(String channel, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Busca notificaciones por email de destinatario
     */
    List<Notification> findByRecipientEmailOrderBySentAtDesc(String email);

    /**
     * Busca notificaciones por número de teléfono de destinatario
     */
    List<Notification> findByRecipientPhoneOrderBySentAtDesc(String phone);
}
