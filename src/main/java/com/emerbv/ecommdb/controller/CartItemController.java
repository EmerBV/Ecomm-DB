package com.emerbv.ecommdb.controller;

import com.emerbv.ecommdb.dto.CartDto;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.Cart;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.response.ApiResponse;
import com.emerbv.ecommdb.service.cart.ICartItemService;
import com.emerbv.ecommdb.service.cart.ICartService;
import com.emerbv.ecommdb.service.user.IUserService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/cartItems")
public class CartItemController {
    private final ICartItemService cartItemService;
    private final ICartService cartService;
    private final IUserService userService;

    /*
    @PostMapping("/item/add")
    public ResponseEntity<ApiResponse> addItemToCart(
            @RequestParam Long productId,
            @RequestParam Integer quantity
    ) {
        try {
            // Para añadirlo manualmente y hacer pruebas
            //User user = userService.getUserById(1L);

            User user = userService.getAuthenticatedUser();
            Cart cart = cartService.initializeNewCart(user);
            cartItemService.addItemToCart(cart.getId(), productId, quantity);
            return ResponseEntity.ok(new ApiResponse("Add Item Success", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        } catch (JwtException e) {
            return  ResponseEntity.status(UNAUTHORIZED).body(new ApiResponse(e.getMessage(), null));
        }
    }
     */

    @PostMapping("/item/add")
    public ResponseEntity<ApiResponse> addItemToCart(
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) Long variantId
    ) {
        try {
            User user = userService.getAuthenticatedUser();
            Cart cart = cartService.initializeNewCart(user);

            if (variantId != null) {
                cartItemService.addItemToCartWithVariant(cart.getId(), productId, variantId, quantity);
            } else {
                cartItemService.addItemToCartWithoutVariant(cart.getId(), productId, quantity);
            }

            // Get the updated cart to return as part of the response
            Cart updatedCart = cartService.getCartByUserId(user.getId());
            CartDto cartDto = cartService.convertToDto(updatedCart);

            return ResponseEntity.ok(new ApiResponse("Add Item Success", cartDto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        } catch (JwtException e) {
            return ResponseEntity.status(UNAUTHORIZED).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @DeleteMapping("/cart/{cartId}/item/{productId}/remove")
    public ResponseEntity<ApiResponse> removeItemFromCart(
            @PathVariable Long cartId,
            @PathVariable Long productId
    ) {
        try {
            cartItemService.removeItemFromCart(cartId, productId);

            // Get the updated cart
            Cart updatedCart = cartService.getCart(cartId);
            CartDto cartDto = cartService.convertToDto(updatedCart);

            return ResponseEntity.ok(new ApiResponse("Remove Item Success", cartDto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PutMapping("/cart/{cartId}/item/{productId}/update")
    public ResponseEntity<ApiResponse> updateItemQuantity(
            @PathVariable Long cartId,
            @PathVariable Long productId,
            @RequestParam Integer quantity
    ) {
        try {
            cartItemService.updateItemQuantity(cartId, productId, quantity);

            // Get the updated cart after the change
            Cart updatedCart = cartService.getCart(cartId);
            CartDto cartDto = cartService.convertToDto(updatedCart);

            return ResponseEntity.ok(new ApiResponse("Update Item Success", cartDto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }
}
