package com.emerbv.ecommdb.request;

import lombok.Data;

@Data
public class PayPalPaymentRequest {
    private Long orderId;
    private String currency;
    private String returnUrl;
    private String cancelUrl;
    private String idempotencyKey;
}
