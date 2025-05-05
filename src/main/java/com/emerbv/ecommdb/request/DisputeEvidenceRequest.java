package com.emerbv.ecommdb.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DisputeEvidenceRequest {
    private String productDescription;
    private String customerEmailAddress;
    private String customerPurchaseIp;
    private LocalDate serviceDate;
    private String shippingDocumentation;
    private String shippingTrackingNumber;
    private String billingAddress;
    private String shippingAddress;
    private LocalDate shippingDate;
    private String shippingCarrier;
    private String customerSignature;
    private String customerCommunication;
    private String refundPolicyDisclosure;
    private String refundRefusalExplanation;
    private String cancellationPolicy;
    private String cancellationPolicyDisclosure;
    private String uncategorizedText;
}
