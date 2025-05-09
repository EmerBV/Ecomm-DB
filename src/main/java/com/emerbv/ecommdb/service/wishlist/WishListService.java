package com.emerbv.ecommdb.service.wishlist;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishListService implements IWishListService {

    private static final Logger logger = LoggerFactory.getLogger(WishListService.class);

    private final WishListRepository wishListRepository;
    private final IUserService userService;
    private final IProductService productService;
    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public WishListDto getUserWishList(Long userId) {
        try {
            User user = userService.getUserById(userId);
            WishList wishList = initializeNewWishList(user);
            return convertToDto(wishList);
        } catch (ResourceNotFoundException e) {
            logger.error("User not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error getting wishlist for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Could not retrieve wishlist: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public WishListDto addProductToWishList(Long userId, Long productId) {
        try {
            User user = userService.getUserById(userId);
            Product product = productService.getProductById(productId);

            WishList wishList = initializeNewWishList(user);

            if (!wishList.containsProduct(productId)) {
                wishList.addProduct(product);

                // Incrementar el contador de "wish" del producto
                product.setWishCount(product.getWishCount() + 1);
                productRepository.save(product);

                wishListRepository.save(wishList);
            }

            return convertToDto(wishList);
        } catch (ResourceNotFoundException e) {
            logger.error("Resource not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error adding product {} to wishlist for user {}: {}",
                    productId, userId, e.getMessage(), e);
            throw new RuntimeException("Could not add product to wishlist: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public WishListDto removeProductFromWishList(Long userId, Long productId) {
        try {
            User user = userService.getUserById(userId);
            Product product = productService.getProductById(productId);

            WishList wishList = getWishListByUserId(userId);
            if (wishList == null) {
                throw new ResourceNotFoundException("WishList not found for user: " + userId);
            }

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
        } catch (ResourceNotFoundException e) {
            logger.error("Resource not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error removing product {} from wishlist for user {}: {}",
                    productId, userId, e.getMessage(), e);
            throw new RuntimeException("Could not remove product from wishlist: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProductInWishList(Long userId, Long productId) {
        try {
            return wishListRepository.existsByUserIdAndProductsId(userId, productId);
        } catch (Exception e) {
            logger.error("Error checking if product {} is in wishlist for user {}: {}",
                    productId, userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public void clearWishList(Long userId) {
        try {
            WishList wishList = getWishListByUserId(userId);
            if (wishList == null) {
                throw new ResourceNotFoundException("WishList not found for user: " + userId);
            }

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
        } catch (ResourceNotFoundException e) {
            logger.error("Resource not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error clearing wishlist for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Could not clear wishlist: " + e.getMessage(), e);
        }
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
        } else {
            wishListDto.setProducts(new HashSet<>());
        }

        return wishListDto;
    }

    public WishList getWishList(Long wishListId) {
        return wishListRepository.findById(wishListId)
                .orElseThrow(() -> new ResourceNotFoundException("WishList not found"));
    }

    public WishList getWishListByUserId(Long userId) {
        return wishListRepository.findByUserId(userId).orElse(null);
    }

    @Transactional
    public WishList initializeNewWishList(User user) {
        return Optional.ofNullable(getWishListByUserId(user.getId()))
                .orElseGet(() -> {
                    WishList wishList = new WishList();
                    wishList.setUser(user);
                    wishList.setProducts(new HashSet<>());
                    wishList.setCreatedAt(LocalDateTime.now());
                    return wishListRepository.save(wishList);
                });
    }
}
