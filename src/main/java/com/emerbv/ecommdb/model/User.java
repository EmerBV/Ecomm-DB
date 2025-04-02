package com.emerbv.ecommdb.model;

import com.emerbv.ecommdb.model.common.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.NaturalId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@Getter
@Setter
@Entity
public class User extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String firstName;
    private String lastName;
    @NaturalId
    private String email;
    private String password;

    /**
     * ID del cliente en Stripe, necesario para guardar métodos de pago
     */
    private String stripeCustomerId;

    //@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ShippingDetails> shippingDetails = new ArrayList<>();

    //@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Cart cart;

    //@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Order> orders;

    /**
     * Relación con los métodos de pago del usuario
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CustomerPaymentMethod> paymentMethods = new ArrayList<>();

    @ManyToMany(
            fetch = FetchType.EAGER,
            cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH }
    )

    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    private Collection<Role> roles = new HashSet<>();

    public User() {
        // Constructor vacío necesario para JPA
    }

    public User(
            String firstName,
            String lastName,
            String email,
            String password
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }

    /**
     * Método de conveniencia para añadir un método de pago
     * @param paymentMethod El método de pago a añadir
     * @return El método de pago añadido
     */
    public CustomerPaymentMethod addPaymentMethod(CustomerPaymentMethod paymentMethod) {
        paymentMethods.add(paymentMethod);
        paymentMethod.setUser(this);
        return paymentMethod;
    }

    /**
     * Método de conveniencia para quitar un método de pago
     * @param paymentMethod El método de pago a quitar
     */
    public void removePaymentMethod(CustomerPaymentMethod paymentMethod) {
        paymentMethods.remove(paymentMethod);
        paymentMethod.setUser(null);
    }

    /**
     * Obtiene el método de pago predeterminado del usuario
     * @return El método de pago predeterminado o null si no existe
     */
    public CustomerPaymentMethod getDefaultPaymentMethod() {
        return paymentMethods.stream()
                .filter(CustomerPaymentMethod::isDefault)
                .findFirst()
                .orElse(null);
    }
}
