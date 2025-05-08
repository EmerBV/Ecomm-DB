package com.emerbv.ecommdb.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayPalPaymentResponse {
    private String payPalOrderId;
    private String status;
    private String approvalUrl;
    private String referenceId;
    private BigDecimal amount;
    private String currency;
}
