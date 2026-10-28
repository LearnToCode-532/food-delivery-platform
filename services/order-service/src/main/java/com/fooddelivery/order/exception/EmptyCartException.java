package com.fooddelivery.order.exception;

public class EmptyCartException extends RuntimeException {
    public EmptyCartException() {
        super("Cart is empty; order cannot be created");
    }
}
