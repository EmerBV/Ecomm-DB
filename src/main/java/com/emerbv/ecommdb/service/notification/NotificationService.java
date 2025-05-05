package com.emerbv.ecommdb.service.notification;

import com.emerbv.ecommdb.dto.NotificationDto;
import com.emerbv.ecommdb.enums.NotificationType;
import com.emerbv.ecommdb.exceptions.NotificationException;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Servicio principal de notificaciones que coordina los distintos canales
 * y decide qué canal usar según las preferencias del usuario
 */
@Service
@RequiredArgsConstructor
public class NotificationService implements INotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final EmailService emailService;
    private final SmsService smsService;
    private final NotificationTemplateService templateService;
    private final NotificationRepository notificationRepository;

    @Value("${notification.default-channel:EMAIL}")
    private String defaultChannel;

    @Override
    @Async("notificationTaskExecutor")
    public void sendNotification(NotificationDto notification) {
        logger.info("Procesando notificación: tipo={}, destinatario={}",
                notification.getType(), notification.getRecipientEmail());

        try {
            // Determinar el canal basado en preferencias del usuario o tipo de notificación
            String channel = determineNotificationChannel(notification);

            // Preparar el contenido usando plantillas
            String content = templateService.processTemplate(
                    notification.getType(),
                    notification.getLanguage(),
                    notification.getVariables()
            );

            // Enviar según el canal seleccionado
            switch (channel.toUpperCase()) {
                case "EMAIL":
                    emailService.sendEmail(
                            notification.getRecipientEmail(),
                            notification.getSubject(),
                            content
                    );
                    break;
                case "SMS":
                    if (notification.getPhoneNumber() != null) {
                        smsService.sendSms(
                                notification.getPhoneNumber(),
                                templateService.getSmsSummary(notification.getType(), notification.getLanguage(), notification.getVariables())
                        );
                    } else {
                        logger.warn("No se pudo enviar SMS: número de teléfono no disponible");
                    }
                    break;
                default:
                    logger.warn("Canal de notificación no soportado: {}", channel);
                    break;
            }

            // Guardar registro de la notificación
            saveNotificationRecord(notification, content, channel, true);

        } catch (Exception e) {
            logger.error("Error al enviar notificación: {}", e.getMessage(), e);
            // Guardar registro del error
            saveNotificationRecord(notification, e.getMessage(), notification.getChannel(), false);
            throw new NotificationException("Error al enviar notificación", e);
        }
    }

    @Override
    @Async("notificationTaskExecutor")
    public void sendUserNotification(User user, NotificationType type, String subject, String language,
                                     java.util.Map<String, Object> variables) {

        NotificationDto notification = NotificationDto.builder()
                .recipientEmail(user.getEmail())
                .recipientName(user.getFirstName() + " " + user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .userId(user.getId())
                .type(type)
                .subject(subject)
                .language(language)
                .variables(variables)
                .build();

        sendNotification(notification);
    }

    private String determineNotificationChannel(NotificationDto notification) {
        // Si la notificación especifica un canal, usar ese
        if (notification.getChannel() != null && !notification.getChannel().isEmpty()) {
            return notification.getChannel();
        }

        // Buscar preferencias del usuario si está disponible
        if (notification.getUserId() != null) {
            // Se podría implementar un servicio que consulte las preferencias del usuario
            // por ahora retornamos el canal por defecto
        }

        // Para algunos tipos de notificación, podríamos tener reglas específicas
        if (notification.getType() == NotificationType.ORDER_SHIPPED) {
            return "SMS";  // Las notificaciones de envío quizás preferimos por SMS
        }

        return defaultChannel;
    }

    private void saveNotificationRecord(NotificationDto notification, String content,
                                        String channel, boolean success) {
        com.emerbv.ecommdb.model.Notification record = new com.emerbv.ecommdb.model.Notification();
        record.setType(notification.getType().name());
        record.setChannel(channel);
        record.setRecipientEmail(notification.getRecipientEmail());
        record.setRecipientPhone(notification.getPhoneNumber());
        record.setSubject(notification.getSubject());
        record.setContent(content);
        record.setUserId(notification.getUserId());
        record.setSuccess(success);
        record.setSentAt(java.time.LocalDateTime.now());

        notificationRepository.save(record);
    }
}
