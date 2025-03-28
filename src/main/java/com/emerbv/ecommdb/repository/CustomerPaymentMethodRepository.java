package com.emerbv.ecommdb.repository;

import com.emerbv.ecommdb.model.CustomerPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerPaymentMethodRepository extends JpaRepository<CustomerPaymentMethod, Long> {

    /**
     * Encuentra todos los métodos de pago de un usuario
     */
    List<CustomerPaymentMethod> findByUserId(Long userId);

    /**
     * Encuentra el método de pago predeterminado de un usuario
     */
    Optional<CustomerPaymentMethod> findByUserIdAndIsDefaultTrue(Long userId);

    /**
     * Encuentra un método de pago por su ID de Stripe
     */
    Optional<CustomerPaymentMethod> findByStripePaymentMethodId(String stripePaymentMethodId);

    /**
     * Verifica si ya existe un método de pago con el ID de Stripe proporcionado
     */
    boolean existsByStripePaymentMethodId(String stripePaymentMethodId);
}
