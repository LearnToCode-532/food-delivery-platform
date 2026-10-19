package com.fooddelivery.delivery.exception;

/**
 * Exception thrown when a Redis GEO operation fails.
 *
 * This is distinct from other exceptions because GEO operations represent
 * a critical read-write capability. Unlike availability updates which can degrade
 * gracefully, location tracking is essential for delivery operations.
 */
public class RedisGeoOperationException extends RuntimeException {

    private RedisGeoOperationException(String message) {
        super(message);
    }

    private RedisGeoOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static RedisGeoOperationException failedToStoreLocation(Long partnerId) {
        return new RedisGeoOperationException(
                "Failed to store location for delivery partner: " + partnerId
        );
    }

    public static RedisGeoOperationException failedToSearchNearby(
            Double latitude,
            Double longitude,
            Double radiusKm) {
        return new RedisGeoOperationException(
                "Failed to search for nearby partners at "
                + latitude + "," + longitude
                + " within " + radiusKm + " km"
        );
    }

    public static RedisGeoOperationException withCause(String message, Throwable cause) {
        return new RedisGeoOperationException(message, cause);
    }
}
