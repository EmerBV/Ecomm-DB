package com.emerbv.ecommdb.service.payment;

import com.emerbv.ecommdb.dto.PaymentMethodDto;

import java.util.List;
import java.util.Optional;

public interface IPaymentMethodService {

    /**
     * Guarda un nuevo método de pago para un usuario
     * @param userId ID del usuario
     * @param paymentMethodId ID del método de pago en Stripe
     * @param setAsDefault Si debe establecerse como método predeterminado
     * @return DTO con la información del método de pago guardado
     */
    PaymentMethodDto savePaymentMethod(Long userId, String paymentMethodId, boolean setAsDefault);

    /**
     * Obtiene todos los métodos de pago de un usuario
     * @param userId ID del usuario
     * @return Lista de DTOs con la información de los métodos de pago
     */
    List<PaymentMethodDto> getUserPaymentMethods(Long userId);

    /**
     * Establece un método de pago como predeterminado
     * @param userId ID del usuario
     * @param paymentMethodId ID del método de pago
     * @return DTO con la información del método de pago actualizado
     */
    PaymentMethodDto setDefaultPaymentMethod(Long userId, Long paymentMethodId);

    /**
     * Elimina un método de pago
     * @param userId ID del usuario
     * @param paymentMethodId ID del método de pago
     */
    void deletePaymentMethod(Long userId, Long paymentMethodId);

    /**
     * Obtiene el método de pago predeterminado del usuario
     * @param userId ID del usuario
     * @return DTO con la información del método de pago predeterminado, o vacío si no hay
     */
    Optional<PaymentMethodDto> getDefaultPaymentMethod(Long userId);
}
