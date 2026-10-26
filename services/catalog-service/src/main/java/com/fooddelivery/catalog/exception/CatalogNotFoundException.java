package com.fooddelivery.catalog.exception;

public class CatalogNotFoundException extends RuntimeException {

    private CatalogNotFoundException(String message) {
        super(message);
    }

    public static CatalogNotFoundException restaurant(Long id) {
        return new CatalogNotFoundException("Restaurant not found with id: " + id);
    }

    public static CatalogNotFoundException menuItem(Long id) {
        return new CatalogNotFoundException("Menu item not found with id: " + id);
    }
}
