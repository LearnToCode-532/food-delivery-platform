package com.fooddelivery.cart.exception;

public class CatalogServiceUnavailableException extends RuntimeException {

    public CatalogServiceUnavailableException() {
        super("Catalog Service is unavailable");
    }
}
