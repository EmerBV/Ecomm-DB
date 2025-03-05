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
