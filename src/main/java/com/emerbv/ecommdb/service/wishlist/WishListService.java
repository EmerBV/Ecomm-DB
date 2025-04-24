package com.emerbv.ecommdb.service.wishlist;

import com.emerbv.ecommdb.dto.ProductDto;
import com.emerbv.ecommdb.dto.WishListDto;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.Product;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.model.WishList;
import com.emerbv.ecommdb.repository.ProductRepository;
import com.emerbv.ecommdb.repository.WishListRepository;
import com.emerbv.ecommdb.service.product.IProductService;
import com.emerbv.ecommdb.service.user.IUserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishListService implements IWishListService {

    private final WishListRepository wishListRepository;
    private final IUserService userService;
    private final IProductService productService;
    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public WishListDto getUserWishList(Long userId) {
        User user = userService.getUserById(userId);
        WishList wishList = getOrCreateWishList(user);
        return convertToDto(wishList);
    }

    @Override
    @Transactional
    public WishListDto addProductToWishList(Long userId, Long productId) {
        User user = userService.getUserById(userId);
        Product product = productService.getProductById(productId);

        WishList wishList = getOrCreateWishList(user);

        if (!wishList.containsProduct(productId)) {
            wishList.addProduct(product);

            // Incrementar el contador de "wish" del producto
            product.setWishCount(product.getWishCount() + 1);
            productRepository.save(product);

            wishListRepository.save(wishList);
        }

        return convertToDto(wishList);
    }

    @Override
    @Transactional
    public WishListDto removeProductFromWishList(Long userId, Long productId) {
        User user = userService.getUserById(userId);
        Product product = productService.getProductById(productId);

        WishList wishList = wishListRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("WishList not found for user: " + userId));

        if (wishList.containsProduct(productId)) {
            wishList.removeProduct(product);

            // Decrementar el contador de "wish" del producto
            int currentCount = product.getWishCount();
            if (currentCount > 0) {
                product.setWishCount(currentCount - 1);
                productRepository.save(product);
            }

            wishListRepository.save(wishList);
        }

        return convertToDto(wishList);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProductInWishList(Long userId, Long productId) {
        return wishListRepository.existsByUserIdAndProductsId(userId, productId);
    }

    @Override
    @Transactional
    public void clearWishList(Long userId) {
        WishList wishList = wishListRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("WishList not found for user: " + userId));

        // Decrementar el contador de "wish" para cada producto
        wishList.getProducts().forEach(product -> {
            int currentCount = product.getWishCount();
            if (currentCount > 0) {
                product.setWishCount(currentCount - 1);
                productRepository.save(product);
            }
        });

        wishList.getProducts().clear();
        wishListRepository.save(wishList);
    }

    @Override
    public WishListDto convertToDto(WishList wishList) {
        WishListDto wishListDto = modelMapper.map(wishList, WishListDto.class);

        if (wishList.getUser() != null) {
            wishListDto.setUserId(wishList.getUser().getId());
        }

        if (wishList.getProducts() != null && !wishList.getProducts().isEmpty()) {
            wishListDto.setProducts(
                    wishList.getProducts().stream()
                            .map(product -> productService.convertToDto(product))
                            .collect(Collectors.toSet())
            );
        }

        return wishListDto;
    }

    // Método de utilidad para obtener o crear una lista de deseos si no existe
    private WishList getOrCreateWishList(User user) {
        return wishListRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    WishList newWishList = new WishList();
                    newWishList.setUser(user);
                    return wishListRepository.save(newWishList);
                });
    }
}
