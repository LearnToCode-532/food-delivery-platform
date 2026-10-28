package com.fooddelivery.cart.exception;

public class InvalidCartRequestException extends RuntimeException {

    public InvalidCartRequestException(String message) {
        super(message);
    }
}
