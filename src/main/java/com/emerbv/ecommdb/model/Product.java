package com.emerbv.ecommdb.model;

import com.emerbv.ecommdb.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String brand;
    private BigDecimal price;
    private int inventory;
    private String description;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "category_id")
    private Category category;

    private int discountPercentage;
    @Enumerated(EnumType.STRING)
    private ProductStatus status;
    private int salesCount;
    private int wishCount;
    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Variant> variants;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images;

    public BigDecimal getEffectivePrice() {
        if (variants != null && !variants.isEmpty()) {
            return  variants.stream()
                    .map(Variant::getPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(price);
        }
        return price;
    }

    public int getTotalInventory() {
        if (variants != null && !variants.isEmpty()) {
            return variants.stream()
                    .mapToInt(Variant::getInventory)
                    .sum();
        }
        return  inventory;
    }

    public void updateProductStatus() {
        if (getTotalInventory() == 0) {
            this.status = ProductStatus.OUT_OF_STOCK;
        } else if (this.status == ProductStatus.OUT_OF_STOCK) {
            this.status = ProductStatus.IN_STOCK;
        }
    }

    public void updateProductDetails() {
        this.price = getEffectivePrice(); // Obtener el precio más bajo de las variantes
        this.inventory = getTotalInventory(); // Sumar el inventario de todas las variantes
        updateProductStatus(); // Actualizar el estado según el inventario
    }

    public Product(
            String name,
            String brand,
            BigDecimal price,
            int inventory,
            String description,
            Category category,
            int discountPercentage,
            ProductStatus status,
            int salesCount,
            int wishCount
    ) {
        this.name = name;
        this.brand = brand;
        this.price = price;
        this.inventory = inventory;
        this.description = description;
        this.category = category;
        this.discountPercentage = discountPercentage;
        this.status = status;
        this.salesCount = salesCount;
        this.wishCount = wishCount;
    }
}
