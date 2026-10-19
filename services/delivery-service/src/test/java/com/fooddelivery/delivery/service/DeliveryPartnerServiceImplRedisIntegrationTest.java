package com.fooddelivery.delivery.service;

import com.fooddelivery.delivery.dto.CreateDeliveryPartnerRequest;
import com.fooddelivery.delivery.entity.DeliveryPartner;
import com.fooddelivery.delivery.entity.DeliveryPartnerStatus;
import com.fooddelivery.delivery.repository.DeliveryPartnerRepository;
import com.fooddelivery.delivery.service.redis.DeliveryPartnerRedisService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DeliveryPartnerServiceImpl with Redis.
 *
 * Tests the integration of MySQL persistence and Redis availability tracking.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
})
@Slf4j
class DeliveryPartnerServiceImplRedisIntegrationTest {

    @Autowired
    private DeliveryPartnerService deliveryPartnerService;

    @Autowired
    private DeliveryPartnerRepository repository;

    @Autowired
    private DeliveryPartnerRedisService redisService;

    @BeforeEach
    void setUp() {
        // Clear database and Redis before each test
        repository.deleteAll();
        redisService.clearAvailablePartners();
    }

    @Test
    void updateAvailability_toAvailable_shouldPersistToMysqlAndAddToRedis() {
        // Given
        CreateDeliveryPartnerRequest request = new CreateDeliveryPartnerRequest();
        request.setUserId(1L);

        DeliveryPartner partner = deliveryPartnerService
                .createDeliveryPartner(request);

        Long partnerId = partner.getId();

        // When
        DeliveryPartner updated = deliveryPartnerService
                .updateAvailability(partnerId, true);

        // Then
        // Verify MySQL update
        assertEquals(
                DeliveryPartnerStatus.AVAILABLE,
                updated.getStatus(),
                "Partner status should be AVAILABLE in MySQL"
        );

        // Verify partner persisted in MySQL
        DeliveryPartner fromDb = deliveryPartnerService
                .getDeliveryPartner(partnerId);

        assertEquals(
                DeliveryPartnerStatus.AVAILABLE,
                fromDb.getStatus(),
                "Partner status should be AVAILABLE when fetched from MySQL"
        );

        // Verify Redis update
        assertTrue(
                redisService.isPartnerAvailable(partnerId),
                "Partner should be in Redis available set"
        );
    }

    @Test
    void updateAvailability_toOffline_shouldPersistToMysqlAndRemoveFromRedis() {
        // Given
        CreateDeliveryPartnerRequest request = new CreateDeliveryPartnerRequest();
        request.setUserId(2L);

        DeliveryPartner partner = deliveryPartnerService
                .createDeliveryPartner(request);

        Long partnerId = partner.getId();

        // First make the partner available
        deliveryPartnerService.updateAvailability(partnerId, true);

        assertTrue(
                redisService.isPartnerAvailable(partnerId),
                "Partner should be available before update"
        );

        // When
        DeliveryPartner updated = deliveryPartnerService
                .updateAvailability(partnerId, false);

        // Then
        // Verify MySQL update
        assertEquals(
                DeliveryPartnerStatus.OFFLINE,
                updated.getStatus(),
                "Partner status should be OFFLINE in MySQL"
        );

        // Verify partner persisted in MySQL
        DeliveryPartner fromDb = deliveryPartnerService
                .getDeliveryPartner(partnerId);

        assertEquals(
                DeliveryPartnerStatus.OFFLINE,
                fromDb.getStatus(),
                "Partner status should be OFFLINE when fetched from MySQL"
        );

        // Verify Redis update
        assertFalse(
                redisService.isPartnerAvailable(partnerId),
                "Partner should not be in Redis available set"
        );
    }

    @Test
    void updateAvailability_suspendedPartner_shouldNotChangeStatus() {
        // Given
        CreateDeliveryPartnerRequest request = new CreateDeliveryPartnerRequest();
        request.setUserId(3L);

        DeliveryPartner partner = deliveryPartnerService
                .createDeliveryPartner(request);

        Long partnerId = partner.getId();

        // Manually set status to SUSPENDED in database
        partner.setStatus(DeliveryPartnerStatus.SUSPENDED);
        repository.save(partner);

        // When & Then
        assertThrows(
                IllegalStateException.class,
                () -> deliveryPartnerService.updateAvailability(partnerId, true),
                "Should throw exception when updating suspended partner"
        );
    }

    @Test
    void updateAvailability_busyToOffline_shouldNotAllowChange() {
        // Given
        CreateDeliveryPartnerRequest request = new CreateDeliveryPartnerRequest();
        request.setUserId(4L);

        DeliveryPartner partner = deliveryPartnerService
                .createDeliveryPartner(request);

        Long partnerId = partner.getId();

        // Manually set status to BUSY in database
        partner.setStatus(DeliveryPartnerStatus.BUSY);
        repository.save(partner);

        // When & Then
        assertThrows(
                IllegalStateException.class,
                () -> deliveryPartnerService.updateAvailability(partnerId, false),
                "Should throw exception when trying to set busy partner to offline"
        );
    }

    @Test
    void updateAvailability_busyToAvailable_shouldSucceed() {
        // Given
        CreateDeliveryPartnerRequest request = new CreateDeliveryPartnerRequest();
        request.setUserId(5L);

        DeliveryPartner partner = deliveryPartnerService
                .createDeliveryPartner(request);

        Long partnerId = partner.getId();

        // Manually set status to BUSY in database
        partner.setStatus(DeliveryPartnerStatus.BUSY);
        repository.save(partner);

        // When
        DeliveryPartner updated = deliveryPartnerService
                .updateAvailability(partnerId, true);

        // Then
        assertEquals(
                DeliveryPartnerStatus.AVAILABLE,
                updated.getStatus(),
                "Partner status should change from BUSY to AVAILABLE"
        );

        assertTrue(
                redisService.isPartnerAvailable(partnerId),
                "Partner should be in Redis available set"
        );
    }

    @Test
    void updateAvailability_availableToBusy_shouldRemoveFromRedis() {
        // Given
        CreateDeliveryPartnerRequest request = new CreateDeliveryPartnerRequest();
        request.setUserId(6L);

        DeliveryPartner partner = deliveryPartnerService
                .createDeliveryPartner(request);

        Long partnerId = partner.getId();

        // Make partner available first
        deliveryPartnerService.updateAvailability(partnerId, true);

        assertTrue(
                redisService.isPartnerAvailable(partnerId),
                "Partner should be available"
        );

        // When - set to BUSY via database (simulating delivery assignment)
        DeliveryPartner fromDb = repository.findById(partnerId).get();
        fromDb.setStatus(DeliveryPartnerStatus.BUSY);
        repository.save(fromDb);

        // Then - manually trigger Redis update
        redisService.removeAvailablePartner(partnerId);

        assertFalse(
                redisService.isPartnerAvailable(partnerId),
                "Partner should be removed from Redis when going to BUSY"
        );
    }

    @Test
    void updateAvailability_multipleTransitions_shouldMaintainCorrectRedisState() {
        // Given
        CreateDeliveryPartnerRequest request = new CreateDeliveryPartnerRequest();
        request.setUserId(7L);

        DeliveryPartner partner = deliveryPartnerService
                .createDeliveryPartner(request);

        Long partnerId = partner.getId();

        // When & Then - Transition 1: OFFLINE to AVAILABLE
        deliveryPartnerService.updateAvailability(partnerId, true);
        assertTrue(redisService.isPartnerAvailable(partnerId));

        // When & Then - Transition 2: AVAILABLE to OFFLINE
        deliveryPartnerService.updateAvailability(partnerId, false);
        assertFalse(redisService.isPartnerAvailable(partnerId));

        // When & Then - Transition 3: OFFLINE to AVAILABLE again
        deliveryPartnerService.updateAvailability(partnerId, true);
        assertTrue(redisService.isPartnerAvailable(partnerId));
    }

    @Test
    void updateAvailability_redisFails_shouldStillUpdateMysql() {
        // This test verifies the graceful degradation behavior.
        // Redis failures should not prevent MySQL updates.

        // Given
        CreateDeliveryPartnerRequest request = new CreateDeliveryPartnerRequest();
        request.setUserId(8L);

        DeliveryPartner partner = deliveryPartnerService
                .createDeliveryPartner(request);

        Long partnerId = partner.getId();

        // When
        // Even if Redis operations fail, MySQL should still be updated
        DeliveryPartner updated = deliveryPartnerService
                .updateAvailability(partnerId, true);

        // Then
        // Verify MySQL was updated regardless of Redis state
        assertEquals(
                DeliveryPartnerStatus.AVAILABLE,
                updated.getStatus(),
                "MySQL should be updated even if Redis operations fail"
        );

        // Verify the update persisted
        DeliveryPartner fromDb = repository.findById(partnerId).get();
        assertEquals(
                DeliveryPartnerStatus.AVAILABLE,
                fromDb.getStatus()
        );
    }

    @Test
    void createDeliveryPartner_shouldStartOffline_notInRedis() {
        // Given
        CreateDeliveryPartnerRequest request = new CreateDeliveryPartnerRequest();
        request.setUserId(9L);

        // When
        DeliveryPartner partner = deliveryPartnerService
                .createDeliveryPartner(request);

        // Then
        // New partners should start as OFFLINE
        assertEquals(
                DeliveryPartnerStatus.OFFLINE,
                partner.getStatus(),
                "New partner should start as OFFLINE"
        );

        // New partner should not be in Redis set
        assertFalse(
                redisService.isPartnerAvailable(partner.getId()),
                "New offline partner should not be in Redis"
        );
    }

    @Test
    void concurrentAvailabilityUpdates_shouldMaintainConsistency() {
        // Given
        CreateDeliveryPartnerRequest request1 = new CreateDeliveryPartnerRequest();
        request1.setUserId(10L);

        CreateDeliveryPartnerRequest request2 = new CreateDeliveryPartnerRequest();
        request2.setUserId(11L);

        DeliveryPartner partner1 = deliveryPartnerService
                .createDeliveryPartner(request1);
        DeliveryPartner partner2 = deliveryPartnerService
                .createDeliveryPartner(request2);

        Long partnerId1 = partner1.getId();
        Long partnerId2 = partner2.getId();

        // When - Update both partners to available
        deliveryPartnerService.updateAvailability(partnerId1, true);
        deliveryPartnerService.updateAvailability(partnerId2, true);

        // Then
        assertTrue(redisService.isPartnerAvailable(partnerId1));
        assertTrue(redisService.isPartnerAvailable(partnerId2));

        // When - Update first partner to offline
        deliveryPartnerService.updateAvailability(partnerId1, false);

        // Then - Only first should be offline
        assertFalse(redisService.isPartnerAvailable(partnerId1));
        assertTrue(redisService.isPartnerAvailable(partnerId2));
    }
}
