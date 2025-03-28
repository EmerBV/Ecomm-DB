package com.emerbv.ecommdb.service.payment;

import com.emerbv.ecommdb.dto.PaymentMethodDto;
import com.emerbv.ecommdb.exceptions.AlreadyExistsException;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.exceptions.StripeException;
import com.emerbv.ecommdb.model.CustomerPaymentMethod;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.repository.CustomerPaymentMethodRepository;
import com.emerbv.ecommdb.service.user.IUserService;
import com.stripe.Stripe;
import com.stripe.model.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentMethodService implements IPaymentMethodService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentMethodService.class);

    private final CustomerPaymentMethodRepository paymentMethodRepository;
    private final IUserService userService;
    private final ModelMapper modelMapper;
    private final StripeCustomerService stripeCustomerService;

    /**
     * Guarda un nuevo método de pago para un usuario
     * El token del método de pago debe ser creado en el frontend usando Stripe Elements
     */
    @Override
    @Transactional
    public PaymentMethodDto savePaymentMethod(Long userId, String paymentMethodId, boolean setAsDefault) {
        // Verificar si el usuario existe
        User user = userService.getUserById(userId);

        // Verificar si ya existe este método de pago
        if (paymentMethodRepository.existsByStripePaymentMethodId(paymentMethodId)) {
            throw new AlreadyExistsException("El método de pago ya está registrado");
        }

        try {
            // Recuperar detalles del método de pago desde Stripe
            PaymentMethod stripePaymentMethod = PaymentMethod.retrieve(paymentMethodId);

            // Asociar el método de pago con el cliente en Stripe
            // Si el usuario no tiene un customerId de Stripe, se debe crear primero
            if (user.getStripeCustomerId() == null) {
                // Crear el cliente en Stripe y actualizar el User
                String customerId = stripeCustomerService.getOrCreateStripeCustomer(user);

                // Ahora que tenemos el cliente, asociamos el método de pago
                PaymentMethod.retrieve(paymentMethodId).attach(
                        Map.of("customer", customerId)
                );
            } else {
                // Ya existe el cliente, solo asociamos el método de pago
                PaymentMethod.retrieve(paymentMethodId).attach(
                        Map.of("customer", user.getStripeCustomerId())
                );
            }

            // Extraer la información relevante (no sensible)
            String type = stripePaymentMethod.getType();
            String last4 = null;
            String brand = null;
            Long expiryMonth = null;
            Long expiryYear = null;

            // Si es una tarjeta, obtenemos los datos adicionales
            if ("card".equals(type) && stripePaymentMethod.getCard() != null) {
                last4 = stripePaymentMethod.getCard().getLast4();
                brand = stripePaymentMethod.getCard().getBrand();
                expiryMonth = stripePaymentMethod.getCard().getExpMonth();
                expiryYear = stripePaymentMethod.getCard().getExpYear();
            }

            // Si este método debe ser el predeterminado, actualizar los demás
            if (setAsDefault) {
                paymentMethodRepository.findByUserIdAndIsDefaultTrue(userId)
                        .ifPresent(existingDefault -> {
                            existingDefault.setDefault(false);
                            paymentMethodRepository.save(existingDefault);
                        });
            }

            // Crear y guardar el nuevo método de pago
            CustomerPaymentMethod paymentMethod = new CustomerPaymentMethod(
                    user, paymentMethodId, type, last4, brand, expiryMonth, expiryYear, setAsDefault
            );

            CustomerPaymentMethod savedMethod = paymentMethodRepository.save(paymentMethod);

            // Convertir a DTO y devolver
            return convertToDto(savedMethod);

        } catch (Exception e) {
            throw new StripeException("Error al guardar el método de pago: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene todos los métodos de pago de un usuario
     */
    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodDto> getUserPaymentMethods(Long userId) {
        // Verificar si el usuario existe
        userService.getUserById(userId);

        // Obtener todos los métodos de pago del usuario
        List<CustomerPaymentMethod> paymentMethods = paymentMethodRepository.findByUserId(userId);

        // Convertir a DTOs y devolver
        return paymentMethods.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Establece un método de pago como predeterminado
     */
    @Override
    @Transactional
    public PaymentMethodDto setDefaultPaymentMethod(Long userId, Long paymentMethodId) {
        // Verificar si el usuario existe
        userService.getUserById(userId);

        // Obtener el método de pago
        CustomerPaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new ResourceNotFoundException("Método de pago no encontrado"));

        // Verificar que pertenezca al usuario
        if (!paymentMethod.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Método de pago no encontrado para este usuario");
        }

        // Desactivar el método de pago predeterminado actual si existe
        paymentMethodRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(existingDefault -> {
                    existingDefault.setDefault(false);
                    paymentMethodRepository.save(existingDefault);
                });

        // Establecer el nuevo método predeterminado
        paymentMethod.setDefault(true);
        CustomerPaymentMethod updatedMethod = paymentMethodRepository.save(paymentMethod);

        return convertToDto(updatedMethod);
    }

    /**
     * Elimina un método de pago
     */
    @Override
    @Transactional
    public void deletePaymentMethod(Long userId, Long paymentMethodId) {
        // Verificar si el usuario existe
        userService.getUserById(userId);

        // Obtener el método de pago
        CustomerPaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new ResourceNotFoundException("Método de pago no encontrado"));

        // Verificar que pertenezca al usuario
        if (!paymentMethod.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Método de pago no encontrado para este usuario");
        }

        try {
            // Eliminar de Stripe (desasociar del cliente)
            PaymentMethod.retrieve(paymentMethod.getStripePaymentMethodId()).detach();

            // Eliminar de nuestra base de datos
            paymentMethodRepository.delete(paymentMethod);

        } catch (Exception e) {
            throw new StripeException("Error al eliminar el método de pago: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el método de pago predeterminado del usuario
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentMethodDto> getDefaultPaymentMethod(Long userId) {
        // Verificar si el usuario existe
        userService.getUserById(userId);

        // Obtener el método de pago predeterminado
        return paymentMethodRepository.findByUserIdAndIsDefaultTrue(userId)
                .map(this::convertToDto);
    }

    /**
     * Convierte un CustomerPaymentMethod a PaymentMethodDto
     */
    private PaymentMethodDto convertToDto(CustomerPaymentMethod paymentMethod) {
        return modelMapper.map(paymentMethod, PaymentMethodDto.class);
    }
}
