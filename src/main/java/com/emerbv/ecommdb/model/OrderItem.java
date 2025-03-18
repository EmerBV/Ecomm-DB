package com.emerbv.ecommdb.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int quantity;
    private BigDecimal price;

    // Campos para la variante
    private Long variantId;
    private String variantName;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    public OrderItem(
            Order order,
            Product product,
            int quantity,
            BigDecimal price
    ) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }

    // Método para calcular el precio total del ítem
    public BigDecimal getTotalPrice() {
        return this.price.multiply(new BigDecimal(this.quantity));
    }

}
