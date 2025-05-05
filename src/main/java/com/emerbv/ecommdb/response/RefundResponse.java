package com.emerbv.ecommdb.response;

import com.emerbv.ecommdb.enums.RefundStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundResponse {
    private Long id;
    private String stripeRefundId;
    private Long orderId;
    private BigDecimal amount;
    private RefundStatus status;
    private String reason;
    private String description;
    private String currency;
    private String paymentIntentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
