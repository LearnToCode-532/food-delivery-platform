package com.fooddelivery.delivery.service.redis;

import com.fooddelivery.delivery.dto.NearbyPartnerResponse;
import com.fooddelivery.delivery.exception.RedisGeoOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Component for managing Redis GEO-based delivery partner location tracking.
 *
 * Stores delivery partner locations using Redis GEO commands (GEOADD, GEORADIUS).
 * Provides methods to query nearby partners based on geographic coordinates.
 *
 * Data Structure:
 * - Redis Sorted Set (internally): delivery:partners:geo
 * - Members: delivery partner IDs
 * - Scores: Geohash of longitude/latitude
 *
 * Design Decision on Availability Filtering:
 * - GEO search returns all partners within radius
 * - Availability filtering happens in-memory after GEO search
 * - This is simpler than Redis LUA scripts for initial implementation
 * - If performance becomes an issue, can optimize with LUA scripts later
 *
 * Partner Status & Location Update:
 * - OFFLINE: Can update location (partner may be moving to get home)
 * - AVAILABLE: Can update location (actively delivering)
 * - BUSY: Can update location (actively on a delivery)
 * - SUSPENDED: CANNOT update location (enforcement in service layer)
 *
 * Why SUSPENDED cannot update:
 * - SUSPENDED means the account is restricted/banned
 * - Should not participate in location tracking
 * - Prevents unauthorized location data from suspended accounts
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryPartnerGeoRedisService {

    private static final String GEO_KEY = "delivery:partners:geo";

    private final RedisTemplate<String, String> redisTemplate;
    private final DeliveryPartnerRedisService availabilityService;

    /**
     * Stores or updates a delivery partner's location using Redis GEOADD.
     *
     * GEOADD adds location (longitude, latitude) for a member to a sorted set.
     * If the member already exists, the location is updated.
     *
     * Note: Redis GEOADD expects (longitude, latitude), not (latitude, longitude).
     * This is counterintuitive but follows the geographic convention where
     * longitude (East-West) comes before latitude (North-South).
     *
     * @param partnerId The delivery partner ID
     * @param longitude The geographic longitude (-180 to 180)
     * @param latitude The geographic latitude (-90 to 90)
     * @throws RedisGeoOperationException if the operation fails
     */
    public void storeLocation(Long partnerId, Double longitude, Double latitude) {

        try {
            // GEOADD key longitude latitude member
            redisTemplate.opsForGeo()
                    .add(GEO_KEY,
                            new Point(longitude, latitude),
                            partnerId.toString()
                    );

            log.debug(
                    "Stored location for delivery partner {} at ({}, {})",
                    partnerId,
                    latitude,
                    longitude
            );

        } catch (Exception e) {
            log.error(
                    "Failed to store location for delivery partner {} at ({}, {})",
                    partnerId,
                    latitude,
                    longitude,
                    e
            );

            throw RedisGeoOperationException.withCause(
                    "Failed to store location for delivery partner: " + partnerId,
                    e
            );
        }
    }

    /**
     * Finds delivery partners within a geographic radius.
     *
     * Uses Redis GEORADIUS to find all members within the specified distance
     * from a given coordinate point. Results are sorted by distance (ascending).
     *
     * Availability Filtering:
     * - The availability service is checked for each result
     * - Only AVAILABLE partners are included in the response
     * - This ensures only active partners are returned for dispatch
     *
     * @param latitude The search center latitude
     * @param longitude The search center longitude
     * @param radiusKm The search radius in kilometers
     * @return List of NearbyPartnerResponse, sorted by distance ascending
     * @throws RedisGeoOperationException if the operation fails
     */
    public List<NearbyPartnerResponse> findNearbyPartners(
        Double latitude,
        Double longitude,
        Double radiusKm) {

        try {
            Circle circle = new Circle(
                    new Point(longitude, latitude),
                    new Distance(radiusKm, Metrics.KILOMETERS)
            );

            GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                    redisTemplate.opsForGeo().search(
                            GEO_KEY,
                            circle
                    );

            List<NearbyPartnerResponse> availablePartners = new ArrayList<>();

            if (results != null) {
                for (GeoResult<RedisGeoCommands.GeoLocation<String>> result
                        : results.getContent()) {

                    Long partnerId =
                            Long.parseLong(result.getContent().getName());

                    if (availabilityService.isPartnerAvailable(partnerId)) {
                        availablePartners.add(
                                NearbyPartnerResponse.builder()
                                        .partnerId(partnerId)
                                        .distanceKm(
                                                result.getDistance().getValue()
                                        )
                                        .build()
                        );
                    }
                }
            }

            log.debug(
                    "Found {} available partners within {} km of ({}, {})",
                    availablePartners.size(),
                    radiusKm,
                    latitude,
                    longitude
            );

            return availablePartners;

        } catch (Exception e) {
            log.error(
                    "Failed to search for nearby partners at ({}, {}) within {} km",
                    latitude,
                    longitude,
                    radiusKm,
                    e
            );

            throw RedisGeoOperationException.failedToSearchNearby(
                    latitude,
                    longitude,
                    radiusKm
            );
        }
    }

    /**
     * Removes a delivery partner from the GEO index.
     *
     * Used when a partner is deleted from the system.
     *
     * @param partnerId The delivery partner ID
     */
    public void removeLocation(Long partnerId) {

        try {
            redisTemplate.opsForGeo()
                    .remove(GEO_KEY, partnerId.toString());

            log.debug(
                    "Removed location data for delivery partner {}",
                    partnerId
            );

        } catch (Exception e) {
            log.error(
                    "Failed to remove location for delivery partner {}",
                    partnerId,
                    e
            );

            // Log but don't throw - this is cleanup
        }
    }

    /**
     * Clears all location data from Redis.
     *
     * Used for testing/cleanup purposes.
     */
    public void clearAllLocations() {

        try {
            redisTemplate.delete(GEO_KEY);
            log.debug("Cleared all delivery partner locations from Redis");

        } catch (Exception e) {
            log.error("Failed to clear all locations from Redis", e);
        }
    }
}
