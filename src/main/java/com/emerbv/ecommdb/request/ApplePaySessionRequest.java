package com.emerbv.ecommdb.request;

import lombok.Data;

@Data
public class ApplePaySessionRequest {
    private String validationURL;
    private String domain;
}
