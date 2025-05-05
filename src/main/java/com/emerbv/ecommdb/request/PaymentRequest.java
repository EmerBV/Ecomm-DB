package com.emerbv.ecommdb.request;

import lombok.Data;

@Data
public class PaymentRequest {
    private Long orderId;
    private String paymentMethodId; // Para pago nuevo
    private Long savedPaymentMethodId; // Para método guardado
    private String currency;
    private String receiptEmail;
    private String description;
    private boolean savePaymentMethod; // Para guardar el método después del pago

    private String idempotencyKey;
}
