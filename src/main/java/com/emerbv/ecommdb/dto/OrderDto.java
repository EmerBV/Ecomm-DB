package com.emerbv.ecommdb.dto;

import com.emerbv.ecommdb.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDto {
    private Long orderId;
    private Long userId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderDate;

    private BigDecimal totalAmount;
    private OrderStatus status;

    // ID de la dirección de envío asociada
    private Long shippingDetailsId;

    // Información de la dirección de envío
    private String shippingAddress;
    private String shippingCity;
    private String shippingState;
    private String shippingPostalCode;
    private String shippingCountry;
    private String shippingPhoneNumber;
    private String shippingFullName;

    // Información de pago
    private String paymentMethod;
    private String paymentIntentId;

    // Productos en la orden
    private List<OrderItemDto> items;

    // Método de utilidad para calcular el número total de items
    public int getTotalItems() {
        if (items == null) {
            return 0;
        }
        return items.stream()
                .mapToInt(OrderItemDto::getQuantity)
                .sum();
    }
}
