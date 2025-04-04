package com.emerbv.ecommdb.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CartItem> items = new HashSet<>();

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    public void addItem(CartItem item) {
        this.items.add(item);
        item.setCart(this);
        updateTotalAmount();
    }

    public void removeItem(CartItem item) {
        this.items.remove(item);
        item.setCart(null);
        updateTotalAmount();
    }

    public void updateTotalAmount() {
        this.totalAmount = items.stream()
                .map(item -> {
                    // Ensure we're using the CartItem's totalPrice
                    if (item.getTotalPrice() != null) {
                        return item.getTotalPrice();
                    }

                    // If totalPrice isn't set, calculate it from unit price and quantity
                    BigDecimal unitPrice = item.getUnitPrice();
                    if (unitPrice == null) {
                        return BigDecimal.ZERO;
                    }

                    BigDecimal calculatedTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

                    // Update the item's totalPrice to match
                    item.setTotalPrice(calculatedTotal);

                    return calculatedTotal;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void clearCart() {
        this.items.clear();
        updateTotalAmount();
    }
}
