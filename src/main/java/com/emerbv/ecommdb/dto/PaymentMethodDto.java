package com.emerbv.ecommdb.dto;

import lombok.Data;

/**
 * DTO para transferir información de métodos de pago
 * No incluye datos sensibles de tarjetas
 */
@Data
public class PaymentMethodDto {
    private Long id;
    private String type;
    private String last4;
    private String brand;
    private Long expiryMonth;
    private Long expiryYear;
    private boolean isDefault;

    // No incluimos el stripePaymentMethodId por seguridad
    // Solo se usará internamente en el backend
}
