package com.emerbv.ecommdb.service.notification;

import com.emerbv.ecommdb.dto.NotificationDto;
import com.emerbv.ecommdb.enums.NotificationType;
import com.emerbv.ecommdb.model.User;

import java.util.Map;

/**
 * Interfaz que define los métodos para el envío de notificaciones
 */
public interface INotificationService {

    /**
     * Envía una notificación según los parámetros especificados en el DTO
     * @param notification DTO con la información completa de la notificación
     */
    void sendNotification(NotificationDto notification);

    /**
     * Envía una notificación a un usuario registrado
     * @param user Usuario destinatario
     * @param type Tipo de notificación (ORDER_CONFIRMATION, CART_ABANDONED, etc)
     * @param subject Asunto de la notificación (para email)
     * @param language Código del idioma para la plantilla (es, en, etc)
     * @param variables Mapa con variables para la plantilla
     */
    void sendUserNotification(User user, NotificationType type, String subject,
                              String language, Map<String, Object> variables);
}
