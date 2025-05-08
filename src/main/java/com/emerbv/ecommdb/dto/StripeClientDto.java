package com.emerbv.ecommdb.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO que contiene la información necesaria para el cliente de Stripe en el frontend
 */
@Data
public class StripeClientDto {
    private String publicKey;
    private String currency = "eur";
    //private String locale = "templates/notifications/es";
    private String locale = "es";

    private boolean applePayEnabled = true;
    private String applePayMerchantId;
    private String applePayCountry = "ES"; // Cambia según tu país
    private String applePayCurrency = "EUR"; // Cambia según tu moneda
    private String applePayLabel = "APPECOMM"; // Nombre que se mostrará en Apple Pay

    @Getter
    @Setter
    public static class StripePaymentStatus {
        private boolean success;
        private String message;
        private String paymentIntentId;
        private String clientSecret;
        private String status;

        public StripePaymentStatus(boolean success, String message, String paymentIntentId,
                                   String clientSecret, String status) {
            this.success = success;
            this.message = message;
            this.paymentIntentId = paymentIntentId;
            this.clientSecret = clientSecret;
            this.status = status;
        }
    }
}
