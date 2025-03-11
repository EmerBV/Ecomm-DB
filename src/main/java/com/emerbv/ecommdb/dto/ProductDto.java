package com.emerbv.ecommdb.dto;

import com.emerbv.ecommdb.enums.ProductStatus;
import com.emerbv.ecommdb.model.Category;
import com.emerbv.ecommdb.model.Variant;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductDto {
    private Long id;
    private String name;
    private String brand;
    private BigDecimal price;
    private int inventory;
    private String description;
    private Category category;
    private int discountPercentage;
    private ProductStatus status;
    private int salesCount;
    private int wishCount;
    private boolean preOrder;
    private LocalDateTime createdAt;
    private List<Variant> variants;
    private List<ImageDto> images;
}
