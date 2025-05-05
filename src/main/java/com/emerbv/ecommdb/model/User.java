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
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ShippingDetails> shippingDetails = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private WishList wishList;

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
     * Método de conveniencia para añadir una dirección de envío
     * @param shippingDetails La dirección de envío a añadir
     * @return La dirección de envío añadida
     */
    public ShippingDetails addShippingDetails(ShippingDetails shippingDetails) {
        this.shippingDetails.add(shippingDetails);
        shippingDetails.setUser(this);
        return shippingDetails;
    }

    /**
     * Método de conveniencia para quitar una dirección de envío
     * @param shippingDetails La dirección de envío a quitar
     */
    public void removeShippingDetails(ShippingDetails shippingDetails) {
        this.shippingDetails.remove(shippingDetails);
        shippingDetails.setUser(null);
    }

    /**
     * Obtiene la dirección de envío predeterminada del usuario
     * @return La dirección de envío predeterminada o null si no existe
     */
    public ShippingDetails getDefaultShippingDetails() {
        return shippingDetails.stream()
                .filter(ShippingDetails::isDefault)
                .findFirst()
                .orElse(shippingDetails.isEmpty() ? null : shippingDetails.get(0));
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

    // Nuevos campos para soporte de notificaciones

    /**
     * Número de teléfono para notificaciones SMS
     */
    @Column(name = "phone_number")
    private String phoneNumber;

    /**
     * Idioma preferido para comunicaciones
     */
    @Column(name = "preferred_language", length = 2)
    private String preferredLanguage = "es";

    /**
     * Token de dispositivo para notificaciones push
     */
    @Column(name = "push_token")
    private String pushToken;

    /**
     * Indica si el usuario ha verificado su email
     */
    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    /**
     * Fecha de verificación del email
     */
    @Column(name = "email_verified_at")
    private java.time.LocalDateTime emailVerifiedAt;

    /**
     * Indica si el usuario ha verificado su número de teléfono
     */
    @Column(name = "phone_verified")
    private Boolean phoneVerified = false;

    /**
     * Fecha de verificación del teléfono
     */
    @Column(name = "phone_verified_at")
    private java.time.LocalDateTime phoneVerifiedAt;

    /**
     * Preferencias de notificación del usuario
     */
    @OneToMany(mappedBy = "userId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<NotificationPreference> notificationPreferences = new ArrayList<>();

    /**
     * Historial de notificaciones enviadas al usuario
     */
    @OneToMany(mappedBy = "userId", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<Notification> notifications;

    // Métodos de utilidad para gestionar notificaciones

    /**
     * Verifica si el usuario tiene habilitadas las notificaciones para un tipo específico
     */
    public boolean canReceiveNotificationType(String notificationType) {
        NotificationPreference preference = getActiveNotificationPreference();
        if (preference == null) {
            return true; // Si no hay preferencias, asumimos que puede recibir notificaciones
        }

        return preference.isNotificationTypeEnabled(notificationType);
    }

    /**
     * Obtiene la preferencia de notificación activa del usuario
     */
    public NotificationPreference getActiveNotificationPreference() {
        if (notificationPreferences == null || notificationPreferences.isEmpty()) {
            return null;
        }

        return notificationPreferences.get(0); // Asumiendo que solo hay una preferencia por usuario
    }

    /**
     * Obtiene el canal preferido para notificaciones
     */
    public String getPreferredNotificationChannel() {
        NotificationPreference preference = getActiveNotificationPreference();
        if (preference == null) {
            return "EMAIL"; // Por defecto, email
        }

        return preference.getPreferredChannel();
    }

}
