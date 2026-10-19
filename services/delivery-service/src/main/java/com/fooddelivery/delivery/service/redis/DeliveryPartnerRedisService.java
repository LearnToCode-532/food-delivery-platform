package com.fooddelivery.delivery.service.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Component for managing Redis-based delivery partner availability tracking.
 *
 * This component handles all Redis operations for real-time availability updates.
 * It stores only the delivery partner ID (not the entire object) in a Redis Set
 * named "delivery:partners:available".
 *
 * Redis is used as an operational/read-optimization store.
 * MySQL remains the durable source of truth for DeliveryPartner status.
 *
 * Failures in Redis operations are logged but do not prevent MySQL updates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryPartnerRedisService {

    private static final String AVAILABLE_PARTNERS_KEY = "delivery:partners:available";

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Adds a delivery partner ID to the "available" set in Redis.
     * Called when a delivery partner status changes to AVAILABLE.
     *
     * If Redis fails, the error is logged but does not propagate to the caller.
     *
     * @param partnerId The ID of the delivery partner to mark as available
     */
    public void addAvailablePartner(Long partnerId) {

        try {
            redisTemplate.opsForSet()
                    .add(AVAILABLE_PARTNERS_KEY, partnerId.toString());

            log.debug(
                    "Added delivery partner {} to Redis available set",
                    partnerId
            );

        } catch (DataAccessException e) {

            log.error(
                    "Failed to add delivery partner {} to Redis available set. "
                    + "MySQL update will still proceed.",
                    partnerId,
                    e
            );
        }
    }

    /**
     * Removes a delivery partner ID from the "available" set in Redis.
     * Called when a delivery partner status changes from AVAILABLE to OFFLINE.
     *
     * If Redis fails, the error is logged but does not propagate to the caller.
     *
     * @param partnerId The ID of the delivery partner to mark as unavailable
     */
    public void removeAvailablePartner(Long partnerId) {

        try {
            redisTemplate.opsForSet()
                    .remove(AVAILABLE_PARTNERS_KEY, partnerId.toString());

            log.debug(
                    "Removed delivery partner {} from Redis available set",
                    partnerId
            );

        } catch (DataAccessException e) {

            log.error(
                    "Failed to remove delivery partner {} from Redis available set. "
                    + "MySQL update will still proceed.",
                    partnerId,
                    e
            );
        }
    }

    /**
     * Checks if a delivery partner is in the "available" set in Redis.
     * Used for read-optimization queries.
     *
     * If Redis fails, returns false and logs the error.
     *
     * @param partnerId The ID of the delivery partner to check
     * @return true if the partner is in the available set, false otherwise
     */
    public boolean isPartnerAvailable(Long partnerId) {

        try {
            Boolean isMember = redisTemplate.opsForSet()
                    .isMember(AVAILABLE_PARTNERS_KEY, partnerId.toString());

            return isMember != null && isMember;

        } catch (DataAccessException e) {

            log.error(
                    "Failed to check Redis availability status for partner {}. "
                    + "Returning false.",
                    partnerId,
                    e
            );

            return false;
        }
    }

    /**
     * Clears all available partners from Redis.
     * Useful for cleanup or testing purposes.
     *
     * If Redis fails, the error is logged but does not propagate to the caller.
     */
    public void clearAvailablePartners() {

        try {
            redisTemplate.delete(AVAILABLE_PARTNERS_KEY);

            log.debug("Cleared Redis available partners set");

        } catch (DataAccessException e) {

            log.error(
                    "Failed to clear Redis available partners set",
                    e
            );
        }
    }
}
