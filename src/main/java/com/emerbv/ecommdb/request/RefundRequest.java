package com.emerbv.ecommdb.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequest {
    private Long orderId;
    private BigDecimal amount; // null para reembolso total
    private String reason; // duplicate, fraudulent, requested_by_customer, etc.
    private String description;
    private String idempotencyKey;
}
