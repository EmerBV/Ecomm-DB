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

    // Se pueden agregar más campos conforme sea necesario para la interfaz de usuario
    private String currency = "usd";
    private String locale = "es";

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
