package com.fooddelivery.delivery.exception;

public class DeliveryPartnerNotFoundException
        extends RuntimeException {

    private DeliveryPartnerNotFoundException(String message) {
        super(message);
    }

    public static DeliveryPartnerNotFoundException withId(Long id) {
        return new DeliveryPartnerNotFoundException(
                "Delivery partner not found with id: " + id
        );
    }
}