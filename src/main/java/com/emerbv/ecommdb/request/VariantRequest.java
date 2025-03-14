package com.emerbv.ecommdb.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VariantRequest {
    private Long id;

    @NotBlank(message = "Variant name is required")
    @Size(min = 2, max = 50, message = "Variant name must be between 2 and 50 characters")
    private String name;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price cannot be negative")
    private BigDecimal price;

    @Min(value = 0, message = "Inventory cannot be negative")
    private int inventory;
}
