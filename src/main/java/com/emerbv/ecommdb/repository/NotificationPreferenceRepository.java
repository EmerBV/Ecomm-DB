package com.emerbv.ecommdb.repository;

import com.emerbv.ecommdb.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para operaciones con preferencias de notificación
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    /**
     * Encuentra las preferencias de notificación de un usuario específico
     * @param userId ID del usuario
     * @return Lista de preferencias
     */
    List<NotificationPreference> findByUserId(Long userId);

    /**
     * Cuenta el número de usuarios que tienen activadas las notificaciones de una categoría específica
     * @param category Categoría de notificación
     * @return Número de usuarios
     */
    long countByPromotionalEmailsTrue();

    /**
     * Encuentra usuarios que tienen activadas las notificaciones de carrito abandonado
     * @return Lista de preferencias
     */
    List<NotificationPreference> findByCartRemindersTrue();

    /**
     * Encuentra usuarios que tienen activadas las notificaciones de productos de vuelta en stock
     * @return Lista de preferencias
     */
    List<NotificationPreference> findByProductUpdatesTrueAndNotificationsEnabledTrue();
}
