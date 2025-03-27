package com.emerbv.ecommdb.model;

import com.emerbv.ecommdb.model.common.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class PaymentTransaction extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String paymentIntentId;

    @Column(nullable = false)
    private BigDecimal amount;

    private String currency;

    private String status;

    private String paymentMethod;

    private LocalDateTime paymentDate;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private String errorMessage;

    // Constructor para facilitar la creación de transacciones
    public PaymentTransaction(String paymentIntentId, BigDecimal amount, String currency,
                              String status, String paymentMethod, Order order) {
        this.paymentIntentId = paymentIntentId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.order = order;
        this.paymentDate = LocalDateTime.now();
    }
}
