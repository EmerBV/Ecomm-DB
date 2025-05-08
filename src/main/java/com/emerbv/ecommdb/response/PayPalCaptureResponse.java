package com.emerbv.ecommdb.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayPalCaptureResponse {
    private String payPalOrderId;
    private String status;
    private String captureId;
    private String captureStatus;
    private BigDecimal amount;
    private String currency;
}
