package com.fooddelivery.cart.exception;

public class InvalidCatalogResponseException extends RuntimeException {

    public InvalidCatalogResponseException() {
        super("Catalog Service returned an invalid menu item response");
    }
}
