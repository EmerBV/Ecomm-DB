package com.emerbv.ecommdb.request;

import com.emerbv.ecommdb.enums.OrderStatus;
import lombok.Data;

@Data
public class OrderStatusRequest {
    private OrderStatus status;
}
