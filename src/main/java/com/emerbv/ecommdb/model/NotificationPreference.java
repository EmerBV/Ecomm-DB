package com.emerbv.ecommdb.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad para almacenar las preferencias de notificación de los usuarios
 */
@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Canales de notificación: EMAIL, SMS, PUSH
    @Column(name = "preferred_channel", nullable = false)
    private String preferredChannel = "EMAIL";

    // Categorías de notificación (todas habilitadas por defecto)

    // Transaccionales - generalmente no se pueden desactivar
    @Column(name = "order_notifications", nullable = false)
    private boolean orderNotifications = true;

    @Column(name = "payment_notifications", nullable = false)
    private boolean paymentNotifications = true;

    @Column(name = "shipping_notifications", nullable = false)
    private boolean shippingNotifications = true;

    // Marketing - opcionales
    @Column(name = "promotional_emails", nullable = false)
    private boolean promotionalEmails = true;

    @Column(name = "product_updates", nullable = false)
    private boolean productUpdates = true;

    @Column(name = "cart_reminders", nullable = false)
    private boolean cartReminders = true;

    @Column(name = "personalized_recommendations", nullable = false)
    private boolean personalizedRecommendations = true;

    @Column(name = "event_invitations", nullable = false)
    private boolean eventInvitations = true;

    // Preferencias de frecuencia
    @Column(name = "max_emails_per_week")
    private Integer maxEmailsPerWeek = 7; // Ilimitado si es null

    // Preferencias de hora del día
    @Column(name = "preferred_time_start")
    private Integer preferredTimeStart; // Hora del día (0-23)

    @Column(name = "preferred_time_end")
    private Integer preferredTimeEnd; // Hora del día (0-23)

    // Preferencias de idioma
    @Column(name = "preferred_language")
    private String preferredLanguage = "es";

    // Estado general
    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled = true;

    // Motivo de baja si está desactivado
    @Column(name = "unsubscribe_reason")
    private String unsubscribeReason;

    // Auditoría
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Verifica si un tipo específico de notificación está habilitado
     */
    public boolean isNotificationTypeEnabled(String type) {
        if (!notificationsEnabled) {
            return false;
        }

        return switch (type.toUpperCase()) {
            case "ORDER", "ORDER_CONFIRMATION", "ORDER_SHIPPED", "ORDER_DELIVERED" -> orderNotifications;
            case "PAYMENT", "PAYMENT_CONFIRMATION", "PAYMENT_FAILED" -> paymentNotifications;
            case "SHIPPING" -> shippingNotifications;
            case "PROMOTIONAL", "SPECIAL_OFFER" -> promotionalEmails;
            case "PRODUCT", "PRODUCT_BACK_IN_STOCK", "PRICE_DROP" -> productUpdates;
            case "CART", "CART_ABANDONED" -> cartReminders;
            case "RECOMMENDATIONS", "PERSONALIZED_RECOMMENDATIONS" -> personalizedRecommendations;
            case "EVENT", "EVENT_INVITATIONS" -> eventInvitations;
            default -> true; // Por defecto permitimos otras notificaciones del sistema
        };
    }
}
