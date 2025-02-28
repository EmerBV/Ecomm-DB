package com.emerbv.ecommdb.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VariantUpdateRequest {
    private Long id;
    private String name;
    private BigDecimal price;
    private int inventory;
}
