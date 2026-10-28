package com.fooddelivery.cart.exception;

public class MenuItemUnavailableException extends RuntimeException {

    public MenuItemUnavailableException(long menuItemId) {
        super("Menu item " + menuItemId + " is currently unavailable");
    }
}
