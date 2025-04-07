package com.emerbv.ecommdb.service.order;

import com.emerbv.ecommdb.dto.OrderDto;
import com.emerbv.ecommdb.enums.OrderStatus;
import com.emerbv.ecommdb.model.Order;

import java.util.List;

public interface IOrderService {
    Order placeOrder(Long userId, Long shippingDetailsId);
    OrderDto getOrder(Long orderId);
    List<OrderDto> getUserOrders(Long userId);
    OrderDto convertToDto(Order order);
    Order updateOrderStatus(Long orderId, OrderStatus status);
    Order updatePaymentIntent(Long orderId, String paymentIntentId);
    Order updatePaymentDetails(Long orderId, String paymentIntentId, String paymentMethodId);
}
