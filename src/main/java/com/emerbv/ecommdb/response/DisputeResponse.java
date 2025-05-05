package com.emerbv.ecommdb.response;

import com.emerbv.ecommdb.enums.DisputeStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DisputeResponse {
    private Long id;
    private String stripeDisputeId;
    private Long orderId;
    private String paymentIntentId;
    private BigDecimal amount;
    private String reason;
    private DisputeStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime evidenceSubmittedAt;
    private LocalDateTime dueBy;
    private String receiptFileId;
    private String invoiceFileId;
    private String shippingDocumentationFileId;
    private String serviceDocumentationFileId;
    private String additionalFileId;
}
