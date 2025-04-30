package com.emerbv.ecommdb.service.cart;

import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.Cart;
import com.emerbv.ecommdb.model.CartItem;
import com.emerbv.ecommdb.model.Product;
import com.emerbv.ecommdb.model.Variant;
import com.emerbv.ecommdb.repository.CartItemRepository;
import com.emerbv.ecommdb.repository.CartRepository;
import com.emerbv.ecommdb.repository.ProductRepository;
import com.emerbv.ecommdb.service.product.IProductService;
import com.emerbv.ecommdb.service.variant.IVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CartItemService implements ICartItemService {
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final IProductService productService;
    private final IVariantService variantService;
    private final ICartService cartService;

    /*
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
     */

    @Override
    public void addItemToCart(Long cartId, Long productId, int quantity) {
        addItemToCartWithoutVariant(cartId, productId, quantity);
    }

    @Override
    public void addItemToCartWithVariant(Long cartId, Long productId, Long variantId, int quantity) {
        // 1. Get the cart
        Cart cart = cartService.getCart(cartId);

        // 2. Get the product
        Product product = productService.getProductById(productId);

        // 3. Get the variant
        Variant variant = variantService.getVariantById(variantId);

        // 4. Validate variant belongs to product
        if (!variant.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("Variant does not belong to the specified product");
        }

        // 5. Check if the product with this specific variant is already in the cart
        CartItem cartItem = cart.getItems()
                .stream()
                .filter(item -> item.getProduct() != null &&
                        item.getProduct().getId().equals(productId) &&
                        item.getVariantId() != null &&
                        item.getVariantId().equals(variantId))
                .findFirst().orElse(new CartItem());

        // 6. If not, create a new cart item
        if (cartItem.getId() == null) {
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setVariantId(variant.getId());
            cartItem.setVariantName(variant.getName());
            cartItem.setQuantity(quantity);
            cartItem.setUnitPrice(variant.getPrice());
        } else {
            // 7. If yes, update the quantity
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        }

        cartItem.setTotalPrice();
        cart.addItem(cartItem);
        cartItemRepository.save(cartItem);
        cartRepository.save(cart);

        product.updateProductStatus();
        productRepository.save(product);
    }

    @Override
    public void addItemToCartWithoutVariant(Long cartId, Long productId, int quantity) {
        // 1. Get the cart
        Cart cart = cartService.getCart(cartId);

        // 2. Get the product
        Product product = productService.getProductById(productId);

        // 3. Check if the product (without variant) is already in the cart
        CartItem cartItem = cart.getItems()
                .stream()
                .filter(item -> item.getProduct() != null &&
                        item.getProduct().getId().equals(productId) &&
                        item.getVariantId() == null)
                .findFirst().orElse(new CartItem());

        // 4. If not, create a new cart item
        if (cartItem.getId() == null) {
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            cartItem.setUnitPrice(product.getPrice());
        } else {
            // 5. If yes, update the quantity
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        }

        cartItem.setTotalPrice();
        cart.addItem(cartItem);
        cartItemRepository.save(cartItem);
        cartRepository.save(cart);

        product.updateProductStatus();
        productRepository.save(product);
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

        CartItem itemToUpdate = cart.getItems()
                .stream()
                .filter(item -> item.getProduct() != null && item.getProduct().getId().equals(productId))
                .findFirst().orElseThrow(() -> new ResourceNotFoundException("Item not found"));

        // Update the quantity
        itemToUpdate.setQuantity(quantity);

        // Explicitly recalculate the total price
        if (itemToUpdate.getUnitPrice() != null) {
            itemToUpdate.setTotalPrice(itemToUpdate.getUnitPrice().multiply(new BigDecimal(quantity)));
        }

        // Update cart item
        cartItemRepository.save(itemToUpdate);

        // Update the cart's total amount
        cart.updateTotalAmount();
        cartRepository.save(cart);
    }

    @Override
    public CartItem getCartItem(Long cartId, Long productId) {
        Cart cart = cartService.getCart(cartId);
        return  cart.getItems()
                .stream()
                .filter(item -> item.getProduct() != null && item.getProduct().getId().equals(productId))
                .findFirst().orElseThrow(() -> new ResourceNotFoundException("Item not found"));
    }
}
