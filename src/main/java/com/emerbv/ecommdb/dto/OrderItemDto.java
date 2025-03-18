package com.emerbv.ecommdb.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDto {
    private Long productId;
    private String productName;
    private String productBrand;
    private Long variantId;
    private String variantName;
    private int quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
}
