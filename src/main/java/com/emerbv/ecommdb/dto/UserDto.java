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
    private ShippingDetails shippingDetails;
    private CartDto cart;
    private List<OrderDto> orders;
}
