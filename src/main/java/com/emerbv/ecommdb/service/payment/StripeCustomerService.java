package com.emerbv.ecommdb.service.payment;

import com.emerbv.ecommdb.exceptions.StripeException;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.repository.UserRepository;
import com.stripe.model.Customer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para gestionar clientes en Stripe
 */
@Service
@RequiredArgsConstructor
public class StripeCustomerService {
    private static final Logger logger = LoggerFactory.getLogger(StripeCustomerService.class);

    private final UserRepository userRepository;

    /**
     * Crea un cliente en Stripe para un usuario o devuelve el existente
     *
     * @param user Usuario para el que crear el cliente
     * @return ID del cliente en Stripe
     */
    @Transactional
    public String getOrCreateStripeCustomer(User user) {
        // Si el usuario ya tiene un ID de cliente de Stripe, lo devolvemos
        if (user.getStripeCustomerId() != null && !user.getStripeCustomerId().isEmpty()) {
            return user.getStripeCustomerId();
        }

        try {
            // Crear un nuevo cliente en Stripe
            Map<String, Object> customerParams = new HashMap<>();
            customerParams.put("email", user.getEmail());
            customerParams.put("name", user.getFirstName() + " " + user.getLastName());

            // Opcional: añadir más datos si los tienes
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userId", user.getId().toString());
            customerParams.put("metadata", metadata);

            Customer stripeCustomer = Customer.create(customerParams);

            // Guardar el ID del cliente en nuestro usuario
            user.setStripeCustomerId(stripeCustomer.getId());
            userRepository.save(user);

            logger.info("Cliente Stripe creado para el usuario {}: {}", user.getId(), stripeCustomer.getId());

            return stripeCustomer.getId();

        } catch (Exception e) {
            logger.error("Error al crear cliente en Stripe para el usuario {}: {}", user.getId(), e.getMessage());
            throw new StripeException("Error al crear cliente en Stripe: " + e.getMessage(), e);
        }
    }

    /**
     * Actualiza un cliente existente en Stripe
     *
     * @param user Usuario con los datos actualizados
     */
    @Transactional
    public void updateStripeCustomer(User user) {
        if (user.getStripeCustomerId() == null || user.getStripeCustomerId().isEmpty()) {
            // Si no tiene un ID de cliente, lo creamos
            getOrCreateStripeCustomer(user);
            return;
        }

        try {
            // Recuperar el cliente existente
            Customer customer = Customer.retrieve(user.getStripeCustomerId());

            // Preparar parámetros de actualización
            Map<String, Object> updateParams = new HashMap<>();
            updateParams.put("email", user.getEmail());
            updateParams.put("name", user.getFirstName() + " " + user.getLastName());

            // Actualizar el cliente
            customer.update(updateParams);

            logger.info("Cliente Stripe actualizado para el usuario {}: {}", user.getId(), user.getStripeCustomerId());

        } catch (Exception e) {
            logger.error("Error al actualizar cliente en Stripe para el usuario {}: {}", user.getId(), e.getMessage());
            throw new StripeException("Error al actualizar cliente en Stripe: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina un cliente de Stripe
     *
     * @param user Usuario cuyo cliente se eliminará
     */
    @Transactional
    public void deleteStripeCustomer(User user) {
        if (user.getStripeCustomerId() == null || user.getStripeCustomerId().isEmpty()) {
            // No hay cliente que eliminar
            return;
        }

        try {
            // Recuperar y eliminar el cliente
            Customer customer = Customer.retrieve(user.getStripeCustomerId());
            customer.delete();

            // Actualizar el usuario
            user.setStripeCustomerId(null);
            userRepository.save(user);

            logger.info("Cliente Stripe eliminado para el usuario {}", user.getId());

        } catch (Exception e) {
            logger.error("Error al eliminar cliente en Stripe para el usuario {}: {}", user.getId(), e.getMessage());
            throw new StripeException("Error al eliminar cliente en Stripe: " + e.getMessage(), e);
        }
    }
}
