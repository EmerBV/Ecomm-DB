package com.emerbv.ecommdb.model;

import com.emerbv.ecommdb.model.common.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad que almacena la información de los métodos de pago guardados de un cliente.
 * No almacena los datos completos de la tarjeta por motivos de seguridad PCI,
 * sino solo tokens o identificadores proporcionados por Stripe.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customer_payment_methods")
public class CustomerPaymentMethod extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * ID del método de pago en Stripe (nunca almacenamos los datos de tarjeta directamente)
     */
    @Column(nullable = false)
    private String stripePaymentMethodId;

    /**
     * Tipo de método de pago (ej: tarjeta, cuenta bancaria)
     */
    private String type;

    /**
     * Últimos 4 dígitos de la tarjeta (para mostrar al usuario)
     */
    private String last4;

    /**
     * Marca de la tarjeta (Visa, Mastercard, etc.)
     */
    private String brand;

    /**
     * Mes de expiración
     */
    private Long expiryMonth;

    /**
     * Año de expiración
     */
    private Long expiryYear;

    /**
     * Indica si es el método de pago predeterminado del usuario
     */
    private boolean isDefault;

    public CustomerPaymentMethod(User user, String stripePaymentMethodId,
                                 String type, String last4, String brand,
                                 Long expiryMonth, Long expiryYear,
                                 boolean isDefault) {
        this.user = user;
        this.stripePaymentMethodId = stripePaymentMethodId;
        this.type = type;
        this.last4 = last4;
        this.brand = brand;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.isDefault = isDefault;
    }
}
