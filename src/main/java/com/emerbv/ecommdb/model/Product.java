package com.emerbv.ecommdb.model;

import com.emerbv.ecommdb.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String brand;
    private BigDecimal price;
    private int inventory;

    @Column(length = 4000)
    private String description;

    @ManyToOne(cascade = CascadeType.ALL)
    //@ManyToOne(fetch = FetchType.LAZY) // Optimización de carga
    @JoinColumn(name = "category_id")
    private Category category;

    private int discountPercentage;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    private int salesCount;
    private int wishCount;
    private boolean preOrder;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime lastStockUpdate;

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

        return  0;
    }

    public void updateProductDetails() {
        this.price = getEffectivePrice(); // Obtener el precio más bajo de las variantes
        this.inventory = getTotalInventory(); // Sumar el inventario de todas las variantes
    }

    public ProductStatus getProductStatus() {
        // Actualizar inventario primero desde variantes, si existen
        updateProductDetails();

        // Determinar el estado basado en el inventario total
        if (this.getTotalInventory() <= 0) {
            this.status = ProductStatus.OUT_OF_STOCK;
        } else {
            this.status = ProductStatus.IN_STOCK;
        }
        return this.status;
    }

    public ProductStatus updateProductStatus() {
        // Primero actualizar los detalles del producto para tener el inventario total actualizado
        updateProductDetails();

        // Determinar el estado basado en el inventario total
        ProductStatus newStatus = getTotalInventory() > 0 ? ProductStatus.IN_STOCK : ProductStatus.OUT_OF_STOCK;
        this.status = newStatus;
        return newStatus;
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
            int wishCount,
            boolean preOrder
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
        this.preOrder = preOrder;
    }
}
