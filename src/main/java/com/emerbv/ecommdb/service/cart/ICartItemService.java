package com.emerbv.ecommdb.service.cart;

import com.emerbv.ecommdb.model.CartItem;

public interface ICartItemService {
    void addItemToCart(Long cartId, Long productId, int quantity);
    void addItemToCartWithVariant(Long cartId, Long productId, Long variantId, int quantity);
    void addItemToCartWithoutVariant(Long cartId, Long productId, int quantity);
    void removeItemFromCart(Long cartId, Long productId);
    void updateItemQuantity(Long cartId, Long itemId, int quantity);

    CartItem getCartItem(Long cartId, Long productId);
}
