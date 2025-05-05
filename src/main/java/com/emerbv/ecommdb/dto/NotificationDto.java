package com.emerbv.ecommdb.dto;

import com.emerbv.ecommdb.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO para transferencia de datos de notificaciones
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    // Datos del destinatario
    private String recipientEmail;
    private String recipientName;
    private String phoneNumber;
    private Long userId;

    // Contenido de la notificación
    private NotificationType type;
    private String subject;
    private String content;
    private String language;

    // Variables para plantillas
    private Map<String, Object> variables;

    // Canal preferido (email, sms, push)
    private String channel;

    // Para asociar con entidades
    private String relatedEntityType;
    private Long relatedEntityId;

    // Para seguimiento
    private String trackingId;
}
