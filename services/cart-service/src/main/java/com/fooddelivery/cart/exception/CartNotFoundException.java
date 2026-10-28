package com.fooddelivery.cart.exception;

public class CartNotFoundException extends RuntimeException {

    public CartNotFoundException() {
        super("Cart not found for the requested user");
    }
}
