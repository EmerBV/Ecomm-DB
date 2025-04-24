package com.emerbv.ecommdb.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
public class WishListDto {
    private Long id;
    private Long userId;
    private Set<ProductDto> products = new HashSet<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
