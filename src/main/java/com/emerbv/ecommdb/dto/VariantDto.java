package com.emerbv.ecommdb.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VariantDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private int inventory;
}
