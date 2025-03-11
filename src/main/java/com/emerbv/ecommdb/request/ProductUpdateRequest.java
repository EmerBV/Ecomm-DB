package com.emerbv.ecommdb.request;

import com.emerbv.ecommdb.enums.ProductStatus;
import com.emerbv.ecommdb.model.Category;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductUpdateRequest {
    private Long id;
    private String name;
    private String brand;
    private BigDecimal price;
    private int inventory;
    private String description;
    private Category category;
    private int discountPercentage;
    private ProductStatus status;
    private boolean preOrder;
}
