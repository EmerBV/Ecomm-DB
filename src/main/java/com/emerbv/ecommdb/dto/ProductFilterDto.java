package com.emerbv.ecommdb.dto;

import com.emerbv.ecommdb.enums.ProductStatus;
import lombok.Data;
import org.springframework.data.domain.Sort;

import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

@Data
public class ProductFilterDto {
    private String sortBy;
    private ProductStatus availability;
    private String category;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String brand;

    @Min(value = 0, message = "Page must not be negative")
    private int page = 0;

    @Min(value = 1, message = "Size must be at least 1")
    private int size = 20;

    private Sort.Direction direction = Sort.Direction.ASC;
} 