package com.emerbv.ecommdb.dto;

import com.emerbv.ecommdb.model.ShippingDetails;
import lombok.Data;

import java.util.List;

@Data
public class UserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private List<ShippingDetails> shippingDetails;
    private CartDto cart;
    private List<OrderDto> orders;

    // Añadimos la lista de métodos de pago
    private List<PaymentMethodDto> paymentMethods;

    // Método de conveniencia para obtener el método de pago predeterminado
    public PaymentMethodDto getDefaultPaymentMethod() {
        if (paymentMethods == null || paymentMethods.isEmpty()) {
            return null;
        }

        return paymentMethods.stream()
                .filter(PaymentMethodDto::isDefault)
                .findFirst()
                .orElse(null);
    }
}
