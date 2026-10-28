package com.fooddelivery.cart.service;

import com.fooddelivery.cart.dto.CartResponse;

public interface CartService {

    CartResponse createOrGet(Long userId);

    CartResponse getByUserId(Long userId);

    CartResponse getByIdAndUserId(Long cartId, Long userId);

    CartResponse addItem(Long userId, Long cartId, Long menuItemId, Integer quantity);

    CartResponse updateQuantity(Long userId, Long cartId, Long menuItemId, Integer quantity);

    void removeItem(Long userId, Long cartId, Long menuItemId);

    void clear(Long userId, Long cartId);
}
