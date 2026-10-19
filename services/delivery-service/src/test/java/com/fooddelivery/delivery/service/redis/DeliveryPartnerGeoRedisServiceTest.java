package com.fooddelivery.delivery.service.redis;

import com.fooddelivery.delivery.dto.NearbyPartnerResponse;
import com.fooddelivery.delivery.exception.RedisGeoOperationException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DeliveryPartnerGeoRedisService.
 *
 * Tests Redis GEO operations for delivery partner location tracking.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
})
@Slf4j
class DeliveryPartnerGeoRedisServiceTest {

    @Autowired
    private DeliveryPartnerGeoRedisService geoRedisService;

    @Autowired
    private DeliveryPartnerRedisService availabilityService;

    @BeforeEach
    void setUp() {
        // Clear Redis before each test
        geoRedisService.clearAllLocations();
        availabilityService.clearAvailablePartners();
    }

    @Test
    void storeLocation_shouldAddPartnerLocationToGeo() {
        // Given
        Long partnerId = 1L;
        Double latitude = 25.3176;
        Double longitude = 82.9739;

        // When
        geoRedisService.storeLocation(partnerId, longitude, latitude);

        // Then - Verify location was stored by searching nearby
        List<NearbyPartnerResponse> results = geoRedisService.findNearbyPartners(
                latitude, longitude, 1.0);

        assertNotNull(results);
        assertEquals(0, results.size(), "No available partners yet");

        // Make partner available and search again
        availabilityService.addAvailablePartner(partnerId);
        results = geoRedisService.findNearbyPartners(latitude, longitude, 1.0);

        assertEquals(1, results.size(), "Should find 1 available partner");
        assertEquals(partnerId, results.get(0).getPartnerId());
        assertEquals(0.0, results.get(0).getDistanceKm(), 0.1);
    }

    @Test
    void storeLocation_shouldUpdateLocationIfAlreadyExists() {
        // Given
        Long partnerId = 1L;
        Double initialLat = 25.3176;
        Double initialLon = 82.9739;
        Double newLat = 25.3200;
        Double newLon = 82.9800;

        // When - Store initial location
        geoRedisService.storeLocation(partnerId, initialLon, initialLat);
        availabilityService.addAvailablePartner(partnerId);

        List<NearbyPartnerResponse> initialResults = geoRedisService
                .findNearbyPartners(initialLat, initialLon, 1.0);

        assertEquals(1, initialResults.size());

        // Update location
        geoRedisService.storeLocation(partnerId, newLon, newLat);

        // Then - Search from new location
        List<NearbyPartnerResponse> updatedResults = geoRedisService
                .findNearbyPartners(newLat, newLon, 1.0);

        assertEquals(1, updatedResults.size());

        // Partner should no longer be found near the old location
        List<NearbyPartnerResponse> oldLocationResults = geoRedisService
                .findNearbyPartners(initialLat, initialLon, 0.1);

        assertTrue(oldLocationResults.isEmpty());
    }

    @Test
    void findNearbyPartners_shouldReturnPartnersWithinRadius() {
        // Given - Multiple partners at different distances
        Long partner1 = 1L;
        Long partner2 = 2L;
        Long partner3 = 3L;

        Double searchLat = 25.3176;
        Double searchLon = 82.9739;

        // Partner 1: ~0 km away (at center)
        geoRedisService.storeLocation(partner1, searchLon, searchLat);
        availabilityService.addAvailablePartner(partner1);

        // Partner 2: ~2 km away
        geoRedisService.storeLocation(partner2, 82.9739, 25.3367);
        availabilityService.addAvailablePartner(partner2);

        // Partner 3: ~10 km away
        geoRedisService.storeLocation(partner3, 82.9739, 25.5176);
        availabilityService.addAvailablePartner(partner3);

        // When - Search within 5 km radius
        List<NearbyPartnerResponse> results = geoRedisService
                .findNearbyPartners(searchLat, searchLon, 5.0);

        // Then - Should find partners 1 and 2, but not 3
        assertEquals(2, results.size());

        Long resultId1 = results.get(0).getPartnerId();
        Long resultId2 = results.get(1).getPartnerId();

        assertTrue((resultId1 == 1L && resultId2 == 2L) ||
                   (resultId1 == 2L && resultId2 == 1L));

        assertTrue(results.stream().noneMatch(r -> r.getPartnerId() == 3L));
    }

    @Test
    void findNearbyPartners_shouldReturnResultsOrderedByDistance() {
        // Given - Multiple partners at different distances
        Long partner1 = 1L;
        Long partner2 = 2L;
        Long partner3 = 3L;

        Double searchLat = 25.3176;
        Double searchLon = 82.9739;

        geoRedisService.storeLocation(partner1, searchLon, searchLat);
        availabilityService.addAvailablePartner(partner1);

        geoRedisService.storeLocation(partner2, 82.9739, 25.3367);
        availabilityService.addAvailablePartner(partner2);

        geoRedisService.storeLocation(partner3, 82.9739, 25.3376);
        availabilityService.addAvailablePartner(partner3);

        // When - Search within 10 km radius
        List<NearbyPartnerResponse> results = geoRedisService
                .findNearbyPartners(searchLat, searchLon, 10.0);

        // Then - Should be ordered by distance ascending
        assertEquals(3, results.size());

        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(
                    results.get(i).getDistanceKm() <= results.get(i + 1).getDistanceKm(),
                    "Results should be ordered by distance ascending"
            );
        }

        // Verify order: partner1 (closest), partner2, partner3 (farthest)
        assertEquals(partner1, results.get(0).getPartnerId());
        assertEquals(partner2, results.get(1).getPartnerId());
        assertEquals(partner3, results.get(2).getPartnerId());
    }

    @Test
    void findNearbyPartners_shouldExcludeUnavailablePartners() {
        // Given
        Long availablePartner = 1L;
        Long unavailablePartner = 2L;

        Double searchLat = 25.3176;
        Double searchLon = 82.9739;

        // Store locations for both
        geoRedisService.storeLocation(availablePartner, searchLon, searchLat);
        geoRedisService.storeLocation(unavailablePartner, searchLon, searchLat);

        // Mark only one as available
        availabilityService.addAvailablePartner(availablePartner);

        // When
        List<NearbyPartnerResponse> results = geoRedisService
                .findNearbyPartners(searchLat, searchLon, 1.0);

        // Then - Should only include available partner
        assertEquals(1, results.size());
        assertEquals(availablePartner, results.get(0).getPartnerId());
        assertFalse(results.stream().anyMatch(r -> r.getPartnerId() == unavailablePartner));
    }

    @Test
    void findNearbyPartners_shouldReturnEmptyListIfNoPartnersInRadius() {
        // Given - Partner outside search radius
        Long partnerId = 1L;
        Double partnersLat = 25.0000;
        Double partnersLon = 82.0000;

        Double searchLat = 28.0000;  // ~333 km away
        Double searchLon = 77.0000;

        geoRedisService.storeLocation(partnerId, partnersLon, partnersLat);
        availabilityService.addAvailablePartner(partnerId);

        // When - Search within 100 km radius
        List<NearbyPartnerResponse> results = geoRedisService
                .findNearbyPartners(searchLat, searchLon, 100.0);

        // Then
        assertTrue(results.isEmpty());
    }

    @Test
    void removeLocation_shouldRemovePartnerFromGeo() {
        // Given
        Long partnerId = 1L;
        Double latitude = 25.3176;
        Double longitude = 82.9739;

        geoRedisService.storeLocation(partnerId, longitude, latitude);
        availabilityService.addAvailablePartner(partnerId);

        List<NearbyPartnerResponse> beforeRemoval = geoRedisService
                .findNearbyPartners(latitude, longitude, 1.0);

        assertEquals(1, beforeRemoval.size());

        // When
        geoRedisService.removeLocation(partnerId);

        // Then
        List<NearbyPartnerResponse> afterRemoval = geoRedisService
                .findNearbyPartners(latitude, longitude, 1.0);

        assertTrue(afterRemoval.isEmpty());
    }

    @Test
    void clearAllLocations_shouldRemoveAllLocations() {
        // Given - Multiple partners
        geoRedisService.storeLocation(1L, 82.9739, 25.3176);
        geoRedisService.storeLocation(2L, 82.9739, 25.3367);
        geoRedisService.storeLocation(3L, 82.9739, 25.5176);

        availabilityService.addAvailablePartner(1L);
        availabilityService.addAvailablePartner(2L);
        availabilityService.addAvailablePartner(3L);

        List<NearbyPartnerResponse> beforeClear = geoRedisService
                .findNearbyPartners(25.3176, 82.9739, 100.0);

        assertEquals(3, beforeClear.size());

        // When
        geoRedisService.clearAllLocations();

        // Then
        List<NearbyPartnerResponse> afterClear = geoRedisService
                .findNearbyPartners(25.3176, 82.9739, 100.0);

        assertTrue(afterClear.isEmpty());
    }

    @Test
    void storeLocation_shouldHandleEdgeCaseCoordinates() {
        // Given - Edge case coordinates
        Long partner1 = 1L;
        Long partner2 = 2L;
        Long partner3 = 3L;

        // Equator
        geoRedisService.storeLocation(partner1, 0.0, 0.0);
        // Prime Meridian
        geoRedisService.storeLocation(partner2, 0.0, 45.0);
        // Extreme coordinates
        geoRedisService.storeLocation(partner3, 179.9, 82.9);

        availabilityService.addAvailablePartner(partner1);
        availabilityService.addAvailablePartner(partner2);
        availabilityService.addAvailablePartner(partner3);

        // When & Then - Should be retrievable
        List<NearbyPartnerResponse> results = geoRedisService
                .findNearbyPartners(0.0, 0.0, 10000.0);

        assertTrue(results.size() >= 2);
    }

    @Test
    void findNearbyPartners_shouldHandleLargeRadius() {
        // Given - Partner at one location
        Long partnerId = 1L;
        geoRedisService.storeLocation(partnerId, 82.9739, 25.3176);
        availabilityService.addAvailablePartner(partnerId);

        // When - Search with very large radius
        List<NearbyPartnerResponse> results = geoRedisService
                .findNearbyPartners(0.0, 0.0, 20000.0);

        // Then - Should find the partner
        assertEquals(1, results.size());
        assertEquals(partnerId, results.get(0).getPartnerId());
    }
}
