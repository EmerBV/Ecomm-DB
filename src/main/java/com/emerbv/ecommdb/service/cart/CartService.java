package com.emerbv.ecommdb.service.cart;

import com.emerbv.ecommdb.dto.CartDto;
import com.emerbv.ecommdb.dto.CartItemDto;
import com.emerbv.ecommdb.dto.ImageDto;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.Cart;
import com.emerbv.ecommdb.model.CartItem;
import com.emerbv.ecommdb.model.Image;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.repository.CartItemRepository;
import com.emerbv.ecommdb.repository.CartRepository;
import com.emerbv.ecommdb.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService implements ICartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AtomicLong cartIdGenerator = new AtomicLong(0);
    private final ModelMapper modelMapper;
    private final ImageRepository imageRepository;

    @Override
    public Cart getCart(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        BigDecimal totalAmount = cart.getTotalAmount();
        cart.setTotalAmount(totalAmount);
        return cartRepository.save(cart);
    }

    @Transactional
    @Override
    public void clearCart(Long id) {
        Cart cart = getCart(id);
        cartItemRepository.deleteAllByCartId(id);
        cart.clearCart();
        cartRepository.deleteById(id);
    }

    @Override
    public BigDecimal getTotalPrice(Long id) {
        Cart cart = getCart(id);
        return cart.getTotalAmount();
    }

    @Override
    public Cart initializeNewCart(User user) {
        return Optional.ofNullable(getCartByUserId(user.getId()))
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    @Override
    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId);
    }

    @Override
    public CartDto convertToDto(Cart cart) {
        CartDto cartDto = modelMapper.map(cart, CartDto.class);
        if (cart.getItems() != null) {
            cartDto.setItems(cart.getItems().stream()
                    .map(this::convertCartItemToDto)
                    .collect(Collectors.toSet()));
        }
        return cartDto;
    }

    private CartItemDto convertCartItemToDto(CartItem cartItem) {
        CartItemDto cartItemDto = modelMapper.map(cartItem, CartItemDto.class);

        // Si el producto existe, obtener sus imágenes
        if (cartItem.getProduct() != null) {
            List<Image> images = imageRepository.findByProductId(cartItem.getProduct().getId());

            // Convertir las imágenes a DTOs
            List<ImageDto> imageDtos = images.stream()
                    .map(image -> {
                        ImageDto imageDto = new ImageDto();
                        imageDto.setId(image.getId());
                        imageDto.setFileName(image.getFileName());
                        imageDto.setDownloadUrl(image.getDownloadUrl());
                        return imageDto;
                    })
                    .collect(Collectors.toList());

            // Asignar las imágenes al DTO del CartItem
            cartItemDto.setImages(imageDtos);
        }

        return cartItemDto;
    }
}
