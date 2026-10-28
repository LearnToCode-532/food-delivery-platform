package com.fooddelivery.order.exception;

public class CartServiceUnavailableException extends RuntimeException {
    public CartServiceUnavailableException() {
        super("Cart service is unavailable");
    }
}
