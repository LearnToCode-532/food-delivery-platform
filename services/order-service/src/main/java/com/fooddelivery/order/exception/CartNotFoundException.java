package com.fooddelivery.order.exception;

public class CartNotFoundException extends RuntimeException {
    public CartNotFoundException(Long userId, Long cartId) {
        super("Cart " + cartId + " was not found for user " + userId);
    }
}
