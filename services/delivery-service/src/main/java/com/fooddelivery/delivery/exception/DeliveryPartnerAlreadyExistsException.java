package com.fooddelivery.delivery.exception;

public class DeliveryPartnerAlreadyExistsException
        extends RuntimeException {

    private DeliveryPartnerAlreadyExistsException(String message) {
        super(message);
    }

    public static DeliveryPartnerAlreadyExistsException withUserId(
            Long userId) {

        return new DeliveryPartnerAlreadyExistsException(
                "Delivery partner already exists for userId: " + userId
        );
    }
}