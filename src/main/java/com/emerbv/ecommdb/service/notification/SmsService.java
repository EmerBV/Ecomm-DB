package com.emerbv.ecommdb.service.notification;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Servicio para envío de notificaciones vía SMS
 * Implementa integración con Twilio como proveedor de SMS
 */
@Service
@RequiredArgsConstructor
public class SmsService {
    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${notification.sms.twilio.account-sid}")
    private String twilioAccountSid;

    @Value("${notification.sms.twilio.auth-token}")
    private String twilioAuthToken;

    @Value("${notification.sms.twilio.phone-number}")
    private String twilioPhoneNumber;

    /**
     * Envía un SMS al número especificado
     * @param phoneNumber Número de teléfono destino
     * @param message Mensaje a enviar
     */
    public void sendSms(String phoneNumber, String message) {
        if (!smsEnabled) {
            logger.info("Envío de SMS desactivado: {}, {}", phoneNumber, message);
            return;
        }

        // Validación básica del número de teléfono
        if (!isValidPhoneNumber(phoneNumber)) {
            logger.error("Número de teléfono inválido: {}", phoneNumber);
            throw new IllegalArgumentException("Número de teléfono inválido: " + phoneNumber);
        }

        // Validar longitud del mensaje (los SMS tienen límite de caracteres)
        if (message.length() > 160) {
            logger.warn("Mensaje SMS truncado debido a que excede los 160 caracteres");
            message = message.substring(0, 157) + "...";
        }

        final String finalMessage = message;

        CompletableFuture.runAsync(() -> {
            try {
                // Ejemplo de integración con Twilio
                /*
                Com.twilio.Twilio.init(twilioAccountSid, twilioAuthToken);
                com.twilio.rest.api.v2010.account.Message twilioMessage =
                    com.twilio.rest.api.v2010.account.Message.creator(
                        new com.twilio.type.PhoneNumber(phoneNumber),
                        new com.twilio.type.PhoneNumber(twilioPhoneNumber),
                        finalMessage)
                    .create();

                logger.info("SMS enviado correctamente. SID: {}", twilioMessage.getSid());
                */

                // Por ahora solo simulamos el envío
                logger.info("Simulando envío de SMS a {}: {}", phoneNumber, finalMessage);

            } catch (Exception e) {
                logger.error("Error enviando SMS a {}: {}", phoneNumber, e.getMessage(), e);
            }
        });
    }

    /**
     * Valida que el número de teléfono tenga un formato adecuado
     * @param phoneNumber Número a validar
     * @return true si el formato es válido
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        // Implementar validación más robusta según requisitos
        // Por ejemplo, verificar formato internacional E.164
        return phoneNumber != null &&
                phoneNumber.matches("^\\+?[1-9]\\d{1,14}$");
    }
}
