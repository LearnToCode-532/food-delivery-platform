package com.fooddelivery.cart.exception;

public class CatalogItemNotFoundException extends RuntimeException {

    public CatalogItemNotFoundException(long menuItemId) {
        super("Menu item " + menuItemId + " was not found in the Catalog Service");
    }
}
