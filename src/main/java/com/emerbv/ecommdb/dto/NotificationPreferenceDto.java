package com.emerbv.ecommdb.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para transferencia de datos de preferencias de notificación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceDto {

    @Pattern(regexp = "EMAIL|SMS|PUSH", message = "Canal preferido debe ser EMAIL, SMS o PUSH")
    private String preferredChannel;

    // Categorías de notificación
    private Boolean orderNotifications;
    private Boolean paymentNotifications;
    private Boolean shippingNotifications;
    private Boolean promotionalEmails;
    private Boolean productUpdates;
    private Boolean cartReminders;
    private Boolean personalizedRecommendations;
    private Boolean eventInvitations;

    // Preferencias de frecuencia
    @Min(value = 1, message = "El número mínimo de emails por semana es 1")
    @Max(value = 21, message = "El número máximo de emails por semana es 21")
    private Integer maxEmailsPerWeek;

    // Preferencias de hora del día
    @Min(value = 0, message = "La hora debe estar entre 0 y 23")
    @Max(value = 23, message = "La hora debe estar entre 0 y 23")
    private Integer preferredTimeStart;

    @Min(value = 0, message = "La hora debe estar entre 0 y 23")
    @Max(value = 23, message = "La hora debe estar entre 0 y 23")
    private Integer preferredTimeEnd;

    // Preferencias de idioma
    @Pattern(regexp = "es|en|fr|de|it|pt", message = "Idioma no soportado")
    private String preferredLanguage;

    // Estado general
    private Boolean notificationsEnabled;

    // Motivo de baja si está desactivado
    private String unsubscribeReason;
}
