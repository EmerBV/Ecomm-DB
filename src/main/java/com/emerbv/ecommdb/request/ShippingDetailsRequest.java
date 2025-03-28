package com.emerbv.ecommdb.request;

import lombok.Data;

@Data
public class ShippingDetailsRequest {
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phoneNumber;
    private String fullName;
}
