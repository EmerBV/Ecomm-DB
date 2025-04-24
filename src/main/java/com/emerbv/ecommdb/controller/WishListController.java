package com.emerbv.ecommdb.controller;

import com.emerbv.ecommdb.dto.WishListDto;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.response.ApiResponse;
import com.emerbv.ecommdb.service.wishlist.IWishListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/wishlists")
public class WishListController {

    private final IWishListService wishListService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse> getUserWishList(@PathVariable Long userId) {
        try {
            WishListDto wishListDto = wishListService.getUserWishList(userId);
            return ResponseEntity.ok(new ApiResponse("User wishlist retrieved successfully", wishListDto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PostMapping("/user/{userId}/product/{productId}/add")
    public ResponseEntity<ApiResponse> addProductToWishList(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        try {
            WishListDto wishListDto = wishListService.addProductToWishList(userId, productId);
            return ResponseEntity.ok(new ApiResponse("Product added to wishlist successfully", wishListDto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @DeleteMapping("/user/{userId}/product/{productId}/remove")
    public ResponseEntity<ApiResponse> removeProductFromWishList(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        try {
            WishListDto wishListDto = wishListService.removeProductFromWishList(userId, productId);
            return ResponseEntity.ok(new ApiResponse("Product removed from wishlist successfully", wishListDto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/user/{userId}/product/{productId}/check")
    public ResponseEntity<ApiResponse> isProductInWishList(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        boolean isInWishList = wishListService.isProductInWishList(userId, productId);
        return ResponseEntity.ok(new ApiResponse("Product status in wishlist", isInWishList));
    }

    @DeleteMapping("/user/{userId}/clear")
    public ResponseEntity<ApiResponse> clearWishList(@PathVariable Long userId) {
        try {
            wishListService.clearWishList(userId);
            return ResponseEntity.ok(new ApiResponse("Wishlist cleared successfully", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }
}
