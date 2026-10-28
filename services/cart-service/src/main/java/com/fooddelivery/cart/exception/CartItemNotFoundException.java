package com.fooddelivery.cart.exception;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(long menuItemId) {
        super("Menu item " + menuItemId + " is not in this cart");
    }
}
