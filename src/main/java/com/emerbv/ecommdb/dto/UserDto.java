package com.emerbv.ecommdb.dto;

import com.emerbv.ecommdb.model.ShippingDetails;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private List<ShippingDetails> shippingDetails = new ArrayList<>();
    private CartDto cart;
    private List<OrderDto> orders;

    // Añadimos la lista de métodos de pago
    private List<PaymentMethodDto> paymentMethods;

    // Método de conveniencia para obtener la dirección de envío predeterminada
    public ShippingDetails getDefaultShippingDetails() {
        if (shippingDetails == null || shippingDetails.isEmpty()) {
            return null;
        }

        return shippingDetails.stream()
                .filter(ShippingDetails::isDefault)
                .findFirst()
                .orElse(shippingDetails.get(0));
    }

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
