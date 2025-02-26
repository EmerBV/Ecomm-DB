package com.emerbv.ecommdb.service.cart;

import com.emerbv.ecommdb.dto.CartDto;
import com.emerbv.ecommdb.model.Cart;
import com.emerbv.ecommdb.model.User;

import java.math.BigDecimal;

public interface ICartService {
    Cart getCart(Long id);
    void clearCart(Long id);
    BigDecimal getTotalPrice(Long id);
    Cart initializeNewCart(User user);
    Cart getCartByUserId(Long userId);
    CartDto convertToDto(Cart cart);
}
