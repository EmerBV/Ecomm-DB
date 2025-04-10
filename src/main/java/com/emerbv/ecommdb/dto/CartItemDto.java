package com.emerbv.ecommdb.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartItemDto {
    private Long itemId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Long variantId;
    private String variantName;
    private ProductDto product;
    private List<ImageDto> images;
}
