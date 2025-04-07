package com.emerbv.ecommdb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ShippingDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phoneNumber;
    private String fullName;
    private boolean isDefault;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @JsonIgnore
    @OneToMany(mappedBy = "shippingDetails")
    private List<Order> orders = new ArrayList<>();

    /**
     * Método de ayuda para crear una copia completa de esta dirección de envío
     * Útil para crear copias inmutables para órdenes históricas
     * @return Nueva instancia de ShippingDetails con los mismos datos
     */
    public ShippingDetails createCopy() {
        ShippingDetails copy = new ShippingDetails();
        copy.setAddress(this.address);
        copy.setCity(this.city);
        copy.setState(this.state);
        copy.setPostalCode(this.postalCode);
        copy.setCountry(this.country);
        copy.setPhoneNumber(this.phoneNumber);
        copy.setFullName(this.fullName);
        copy.setUser(this.user);
        return copy;
    }
}
