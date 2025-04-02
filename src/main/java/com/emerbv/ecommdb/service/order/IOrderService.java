package com.emerbv.ecommdb.service.order;

import com.emerbv.ecommdb.dto.OrderDto;
import com.emerbv.ecommdb.enums.OrderStatus;
import com.emerbv.ecommdb.model.Order;

import java.util.List;

public interface IOrderService {
    Order placeOrder(Long userId);
    Order placeOrderWithShippingAddress(Long userId, Long shippingAddressId);
    OrderDto getOrder(Long orderId);
    List<OrderDto> getUserOrders(Long userId);
    OrderDto convertToDto(Order order);
    Order updateOrderStatus(Long orderId, OrderStatus status);
}
