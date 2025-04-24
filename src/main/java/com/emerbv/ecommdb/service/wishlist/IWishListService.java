package com.emerbv.ecommdb.service.wishlist;

import com.emerbv.ecommdb.dto.WishListDto;
import com.emerbv.ecommdb.model.WishList;

public interface IWishListService {
    WishListDto getUserWishList(Long userId);
    WishListDto addProductToWishList(Long userId, Long productId);
    WishListDto removeProductFromWishList(Long userId, Long productId);
    boolean isProductInWishList(Long userId, Long productId);
    void clearWishList(Long userId);
    WishListDto convertToDto(WishList wishList);
}
