package com.emerbv.ecommdb.response;

import lombok.Data;

@Data
public class ApplePayMerchantSessionResponse {
    private String merchantSessionIdentifier;
    private String nonce;
    private String merchantIdentifier;
    private String domainName;
    private String displayName;
    private String signature;
    private Long epochTimestamp;
    private Long expiresAt;
    private String initiative;
    private String initiativeContext;
}
