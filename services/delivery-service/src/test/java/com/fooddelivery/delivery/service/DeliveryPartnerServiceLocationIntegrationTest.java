package com.fooddelivery.delivery.service;

import com.fooddelivery.delivery.dto.CreateDeliveryPartnerRequest;
import com.fooddelivery.delivery.dto.NearbyPartnerResponse;
import com.fooddelivery.delivery.entity.DeliveryPartner;
import com.fooddelivery.delivery.entity.DeliveryPartnerStatus;
import com.fooddelivery.delivery.repository.DeliveryPartnerRepository;
import com.fooddelivery.delivery.service.redis.DeliveryPartnerGeoRedisService;
import com.fooddelivery.delivery.service.redis.DeliveryPartnerRedisService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for location tracking in DeliveryPartnerService.
 *
 * Tests the integration of MySQL, Redis availability, and Redis GEO.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
})
@Slf4j
class DeliveryPartnerServiceLocationIntegrationTest {

    @Autowired
    private DeliveryPartnerService deliveryPartnerService;

    @Autowired
    private DeliveryPartnerRepository repository;

    @Autowired
    private DeliveryPartnerRedisService availabilityService;

    @Autowired
    private DeliveryPartnerGeoRedisService geoRedisService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        availabilityService.clearAvailablePartners();
        geoRedisService.clearAllLocations();
    }

    @Test
    void updateLocation_shouldStoreLocationInRedis() {
        // Given
        CreateDeliveryPartnerRequest request = new CreateDeliveryPartnerRequest();
        request.setUserId(1L);

        DeliveryPartner partner = deliveryPartnerService
                .createDeliveryPartner(request);

        Long partnerId = partner.getId();
        Double latitude = 25.3176;
        Double longitude = 82.9739;

        // When
        deliveryPartnerService.updateLocation(partnerId, latitude, longitude);

        // Then - Make partner available and verify location is stored
        deliveryPartnerService.updateAvailability(partnerId, true);

        List<NearbyPartnerResponse> results = deliveryPartnerService
                .findNearbyPartners(latitude, longitude, 1.0);

        assertEquals(1, results.size());
        assertEquals(partnerId, results.get(0).getPartnerId());
    }

    @Test
    void updateLocation_shouldFailForNonexistentPartner() {
        // When & Then
        assertThrows(
                com.fooddelivery.delivery.exception.DeliveryPartnerNotFoundException.class,
                () -> deliveryPartnerService.updateLocation(9999L, 25.3176, 82.9739)
        );
    }

    @Test
    void updateLocation_shouldFailForSuspendedPartner() {
        // Given
        CreateDeliveryPartnerRequest request = new CreateDeliveryPartnerRequest();
        request.setUserId(2L);

        DeliveryPartner partner = deliveryPartnerService
                .createDeliveryPartner(request);

        Long partnerId = partner.getId();

        // Manually set status to SUSPENDED
        partner.setStatus(DeliveryPartnerStatus.SUSPENDED);
        repository.save(partner);

        // When & Then
        assertThrows(
                IllegalStateException.class,
                () -> deliveryPartnerService.updateLocation(
                        partnerId,
                        25.3176,
                        82.9739
                )
        );
    }

    @Test
    void updateLocation_shouldAllowOfflinePartner() {
        // Given - Partner starts OFFLINE
        CreateDeliveryPartnerRequest request = new CreateDeliveryPartnerRequest();
        request.setUserId(3L);

        DeliveryPartner partner = deliveryPartnerService
                .createDeliveryPartner(request);

        Long partnerId = partner.getId();

        assertEquals(DeliveryPartnerStatus.OFFLINE, partner.getStatus());

        // When - Update location while OFFLINE
        assertDoesNotThrow(
                () -> deliveryPartnerService.updateLocation(
                        partnerId,
                        25.3176,
                        82.9739
                )
        );

        // Then - Location should be stored
        deliveryPartnerService.updateAvailability(partnerId, true);
        List<NearbyPartnerResponse> results = deliveryPartnerService
                .findNearbyPartners(25.3176, 82.9739, 1.0);

        assertEquals(1, results.size());
    }

    @Test
    void updateLocation_shouldAllowAvailablePartner() {
        // Given
        CreateDeliveryPartnerRequest request = new CreateDeliveryPartnerRequest();
        request.setUserId(4L);

        DeliveryPartner partner = deliveryPartnerService
                .createDeliveryPartner(request);

        Long partnerId = partner.getId();

        // Make available first
        deliveryPartnerService.updateAvailability(partnerId, true);

        // When
        assertDoesNotThrow(
                () -> deliveryPartnerService.updateLocation(
                        partnerId,
                        25.3176,
                        82.9739
                )
        );

        // Then - Location should be retrievable
        List<NearbyPartnerResponse> results = deliveryPartnerService
                .findNearbyPartners(25.3176, 82.9739, 1.0);

        assertEquals(1, results.size());
    }

    @Test
    void updateLocation_shouldAllowBusyPartner() {
        // Given - Create a partner and manually set to BUSY
        CreateDeliveryPartnerRequest request = new CreateDeliveryPartnerRequest();
        request.setUserId(5L);

        DeliveryPartner partner = deliveryPartnerService
                .createDeliveryPartner(request);

        Long partnerId = partner.getId();

        // Manually set to BUSY
        partner.setStatus(DeliveryPartnerStatus.BUSY);
        repository.save(partner);

        // When
        assertDoesNotThrow(
                () -> deliveryPartnerService.updateLocation(
                        partnerId,
                        25.3176,
                        82.9739
                )
        );
    }

    @Test
    void findNearbyPartners_shouldReturnOnlyAvailablePartners() {
        // Given - Create multiple partners with different statuses
        CreateDeliveryPartnerRequest req1 = new CreateDeliveryPartnerRequest();
        req1.setUserId(10L);
        DeliveryPartner partner1 = deliveryPartnerService.createDeliveryPartner(req1);
        Long partnerId1 = partner1.getId();

        CreateDeliveryPartnerRequest req2 = new CreateDeliveryPartnerRequest();
        req2.setUserId(11L);
        DeliveryPartner partner2 = deliveryPartnerService.createDeliveryPartner(req2);
        Long partnerId2 = partner2.getId();

        Double searchLat = 25.3176;
        Double searchLon = 82.9739;

        // Store locations for both
        deliveryPartnerService.updateLocation(partnerId1, searchLat, searchLon);
        deliveryPartnerService.updateLocation(partnerId2, searchLat, searchLon);

        // Make only first one available
        deliveryPartnerService.updateAvailability(partnerId1, true);

        // When
        List<NearbyPartnerResponse> results = deliveryPartnerService
                .findNearbyPartners(searchLat, searchLon, 1.0);

        // Then - Should only include available partner
        assertEquals(1, results.size());
        assertEquals(partnerId1, results.get(0).getPartnerId());
    }

    @Test
    void findNearbyPartners_shouldExcludeSuspendedPartners() {
        // Given - Create two partners
        CreateDeliveryPartnerRequest req1 = new CreateDeliveryPartnerRequest();
        req1.setUserId(20L);
        DeliveryPartner partner1 = deliveryPartnerService.createDeliveryPartner(req1);
        Long partnerId1 = partner1.getId();

        CreateDeliveryPartnerRequest req2 = new CreateDeliveryPartnerRequest();
        req2.setUserId(21L);
        DeliveryPartner partner2 = deliveryPartnerService.createDeliveryPartner(req2);
        Long partnerId2 = partner2.getId();

        Double searchLat = 25.3176;
        Double searchLon = 82.9739;

        // Store locations and make both available
        deliveryPartnerService.updateLocation(partnerId1, searchLat, searchLon);
        deliveryPartnerService.updateLocation(partnerId2, searchLat, searchLon);

        deliveryPartnerService.updateAvailability(partnerId1, true);
        deliveryPartnerService.updateAvailability(partnerId2, true);

        // Suspend the second partner
        DeliveryPartner p2 = repository.findById(partnerId2).get();
        p2.setStatus(DeliveryPartnerStatus.SUSPENDED);
        repository.save(p2);

        // Also remove from availability set
        availabilityService.removeAvailablePartner(partnerId2);

        // When
        List<NearbyPartnerResponse> results = deliveryPartnerService
                .findNearbyPartners(searchLat, searchLon, 1.0);

        // Then - Should only include non-suspended partner
        assertEquals(1, results.size());
        assertEquals(partnerId1, results.get(0).getPartnerId());
    }

    @Test
    void findNearbyPartners_shouldReturnResultsOrderedByDistance() {
        // Given - Create 3 partners at different distances
        CreateDeliveryPartnerRequest req1 = new CreateDeliveryPartnerRequest();
        req1.setUserId(30L);
        DeliveryPartner partner1 = deliveryPartnerService.createDeliveryPartner(req1);

        CreateDeliveryPartnerRequest req2 = new CreateDeliveryPartnerRequest();
        req2.setUserId(31L);
        DeliveryPartner partner2 = deliveryPartnerService.createDeliveryPartner(req2);

        CreateDeliveryPartnerRequest req3 = new CreateDeliveryPartnerRequest();
        req3.setUserId(32L);
        DeliveryPartner partner3 = deliveryPartnerService.createDeliveryPartner(req3);

        Double searchLat = 25.3176;
        Double searchLon = 82.9739;

        // Partner 1: At center
        deliveryPartnerService.updateLocation(partner1.getId(), searchLat, searchLon);

        // Partner 2: ~2 km away
        deliveryPartnerService.updateLocation(partner2.getId(), searchLat + 0.02, searchLon);

        // Partner 3: ~4 km away
        deliveryPartnerService.updateLocation(partner3.getId(), searchLat + 0.04, searchLon);

        // Make all available
        deliveryPartnerService.updateAvailability(partner1.getId(), true);
        deliveryPartnerService.updateAvailability(partner2.getId(), true);
        deliveryPartnerService.updateAvailability(partner3.getId(), true);

        // When
        List<NearbyPartnerResponse> results = deliveryPartnerService
                .findNearbyPartners(searchLat, searchLon, 10.0);

        // Then - Should be ordered by distance ascending
        assertEquals(3, results.size());

        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(
                    results.get(i).getDistanceKm() <= results.get(i + 1).getDistanceKm(),
                    "Results should be ordered by distance ascending"
            );
        }
    }

    @Test
    void findNearbyPartners_shouldReturnEmptyListWhenNoPartnersInRadius() {
        // Given
        Double searchLat = 25.3176;
        Double searchLon = 82.9739;

        // When
        List<NearbyPartnerResponse> results = deliveryPartnerService
                .findNearbyPartners(searchLat, searchLon, 1.0);

        // Then
        assertTrue(results.isEmpty());
    }

    @Test
    void findNearbyPartners_shouldReturnDistanceInKilometers() {
        // Given
        CreateDeliveryPartnerRequest request = new CreateDeliveryPartnerRequest();
        request.setUserId(40L);

        DeliveryPartner partner = deliveryPartnerService
                .createDeliveryPartner(request);

        Long partnerId = partner.getId();

        Double searchLat = 25.3176;
        Double searchLon = 82.9739;

        // Store location at search point
        deliveryPartnerService.updateLocation(partnerId, searchLat, searchLon);
        deliveryPartnerService.updateAvailability(partnerId, true);

        // When
        List<NearbyPartnerResponse> results = deliveryPartnerService
                .findNearbyPartners(searchLat, searchLon, 1.0);

        // Then - Distance should be close to 0
        assertEquals(1, results.size());
        assertTrue(results.get(0).getDistanceKm() < 0.1);
    }

    @Test
    void updateLocation_multipleUpdates_shouldUpdatePreviousLocation() {
        // Given
        CreateDeliveryPartnerRequest request = new CreateDeliveryPartnerRequest();
        request.setUserId(50L);

        DeliveryPartner partner = deliveryPartnerService
                .createDeliveryPartner(request);

        Long partnerId = partner.getId();

        Double initialLat = 25.3176;
        Double initialLon = 82.9739;
        Double newLat = 25.3200;
        Double newLon = 82.9800;

        deliveryPartnerService.updateAvailability(partnerId, true);

        // When - Update location first time
        deliveryPartnerService.updateLocation(partnerId, initialLat, initialLon);

        List<NearbyPartnerResponse> initialSearch = deliveryPartnerService
                .findNearbyPartners(initialLat, initialLon, 1.0);

        assertEquals(1, initialSearch.size());

        // Update location second time
        deliveryPartnerService.updateLocation(partnerId, newLat, newLon);

        // Search from new location
        List<NearbyPartnerResponse> updatedSearch = deliveryPartnerService
                .findNearbyPartners(newLat, newLon, 1.0);

        // Then - Should still find partner at new location
        assertEquals(1, updatedSearch.size());
        assertTrue(updatedSearch.get(0).getDistanceKm() < 0.1);
    }

    @Test
    void deleteDeliveryPartner_shouldRemoveFromGeo() {
        // Given
        CreateDeliveryPartnerRequest request = new CreateDeliveryPartnerRequest();
        request.setUserId(60L);

        DeliveryPartner partner = deliveryPartnerService
                .createDeliveryPartner(request);

        Long partnerId = partner.getId();
        Double latitude = 25.3176;
        Double longitude = 82.9739;

        deliveryPartnerService.updateLocation(partnerId, latitude, longitude);
        deliveryPartnerService.updateAvailability(partnerId, true);

        List<NearbyPartnerResponse> beforeDelete = deliveryPartnerService
                .findNearbyPartners(latitude, longitude, 1.0);

        assertEquals(1, beforeDelete.size());

        // When
        deliveryPartnerService.deleteDeliveryPartner(partnerId);

        // Then - Partner should be removed from GEO
        List<NearbyPartnerResponse> afterDelete = deliveryPartnerService
                .findNearbyPartners(latitude, longitude, 1.0);

        assertTrue(afterDelete.isEmpty());
    }

    @Test
    void concurrentLocationUpdates_shouldMaintainConsistency() {
        // Given - Create 3 partners
        CreateDeliveryPartnerRequest req1 = new CreateDeliveryPartnerRequest();
        req1.setUserId(70L);
        DeliveryPartner p1 = deliveryPartnerService.createDeliveryPartner(req1);

        CreateDeliveryPartnerRequest req2 = new CreateDeliveryPartnerRequest();
        req2.setUserId(71L);
        DeliveryPartner p2 = deliveryPartnerService.createDeliveryPartner(req2);

        CreateDeliveryPartnerRequest req3 = new CreateDeliveryPartnerRequest();
        req3.setUserId(72L);
        DeliveryPartner p3 = deliveryPartnerService.createDeliveryPartner(req3);

        Double searchLat = 25.3176;
        Double searchLon = 82.5739;

        // When - Update locations for all
        deliveryPartnerService.updateLocation(p1.getId(), searchLat, searchLon);
        deliveryPartnerService.updateLocation(p2.getId(), searchLat + 0.01, searchLon);
        deliveryPartnerService.updateLocation(p3.getId(), searchLat + 0.02, searchLon);

        deliveryPartnerService.updateAvailability(p1.getId(), true);
        deliveryPartnerService.updateAvailability(p2.getId(), true);
        deliveryPartnerService.updateAvailability(p3.getId(), true);

        // Then - Search should return all with correct distances
        List<NearbyPartnerResponse> results = deliveryPartnerService
                .findNearbyPartners(searchLat, searchLon, 10.0);

        assertEquals(3, results.size());
        assertTrue(results.stream()
                .allMatch(r -> r.getDistanceKm() != null && r.getDistanceKm() >= 0));
    }
}
