package com.emerbv.ecommdb.request;

import lombok.Data;

@Data
public class PaymentRequest {
    private Long orderId;
    private String paymentMethodId;
    private String currency;
    private String receiptEmail;
    private String description;
}
