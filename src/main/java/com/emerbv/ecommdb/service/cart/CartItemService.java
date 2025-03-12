package com.emerbv.ecommdb.service.cart;

import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.Cart;
import com.emerbv.ecommdb.model.CartItem;
import com.emerbv.ecommdb.model.Product;
import com.emerbv.ecommdb.repository.CartItemRepository;
import com.emerbv.ecommdb.repository.CartRepository;
import com.emerbv.ecommdb.service.product.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CartItemService implements ICartItemService {
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final IProductService productService;
    private final ICartService cartService;

    @Override
    public void addItemToCart(Long cartId, Long productId, int quantity) {
        // 1. Get the cart
        Cart cart = cartService.getCart(cartId);

        // 2. Get the product
        Product product = productService.getProductById(productId);
        System.out.println("\n\n=====================================================================================");
        System.out.println("The product Id:" + productId);
        System.out.println("The product:" + product);
        System.out.println("\n\n=====================================================================================");

        // 3. Check if the product already in the cart
        CartItem cartItem = cart.getItems()
                .stream()
                //.filter(item -> item.getProduct().getId().equals(productId))
                .filter(item -> item.getProduct() != null && item.getProduct().getId().equals(productId))
                .findFirst().orElse(new CartItem());

        // 4. If No, then initiate a new CartItem entry
        if (cartItem.getId() == null) {
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            cartItem.setUnitPrice(product.getPrice());
        } else {
            // 5. If Yes, then increase the quantity with the requested quantity
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        }

        cartItem.setTotalPrice();
        cart.addItem(cartItem);
        cartItemRepository.save(cartItem);
        cartRepository.save(cart);
    }

    @Override
    public void removeItemFromCart(Long cartId, Long productId) {
        Cart cart = cartService.getCart(cartId);
        CartItem itemToRemove = getCartItem(cartId, productId);
        cart.removeItem(itemToRemove);
        cartRepository.save(cart);
    }

    @Override
    public void updateItemQuantity(Long cartId, Long productId, int quantity) {
        Cart cart = cartService.getCart(cartId);
        cart.getItems()
                .stream()
                //.filter(item -> item.getProduct().getId().equals(productId))
                .filter(item -> item.getProduct() != null && item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    item.setQuantity(quantity);
                    item.setUnitPrice(item.getProduct().getPrice());
                    item.setTotalPrice();
                });

        BigDecimal totalAmount = cart.getItems()
                .stream().map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalAmount(totalAmount);
        cartRepository.save(cart);
    }

    @Override
    public CartItem getCartItem(Long cartId, Long productId) {
        Cart cart = cartService.getCart(cartId);
        return  cart.getItems()
                .stream()
                //.filter(item -> item.getProduct().getId().equals(productId))
                .filter(item -> item.getProduct() != null && item.getProduct().getId().equals(productId))
                .findFirst().orElseThrow(() -> new ResourceNotFoundException("Item not found"));
    }
}
