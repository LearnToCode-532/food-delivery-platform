package com.fooddelivery.delivery.service.redis;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DeliveryPartnerRedisService.
 *
 * Tests Redis operations for delivery partner availability tracking.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
})
@Slf4j
class DeliveryPartnerRedisServiceTest {

    @Autowired
    private DeliveryPartnerRedisService redisService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String AVAILABLE_PARTNERS_KEY = "delivery:partners:available";
    private static final Long TEST_PARTNER_ID = 123L;

    @BeforeEach
    void setUp() {
        // Clear Redis before each test
        redisService.clearAvailablePartners();
    }

    @Test
    void addAvailablePartner_shouldAddPartnerIdToRedisSet() {
        // When
        redisService.addAvailablePartner(TEST_PARTNER_ID);

        // Then
        Boolean isMember = redisTemplate.opsForSet()
                .isMember(AVAILABLE_PARTNERS_KEY, TEST_PARTNER_ID.toString());

        assertTrue(isMember, "Partner should be added to available set");
    }

    @Test
    void addAvailablePartner_shouldHandleMultiplePartners() {
        // Given
        Long partnerId1 = 1L;
        Long partnerId2 = 2L;
        Long partnerId3 = 3L;

        // When
        redisService.addAvailablePartner(partnerId1);
        redisService.addAvailablePartner(partnerId2);
        redisService.addAvailablePartner(partnerId3);

        // Then
        Long setSize = redisTemplate.opsForSet()
                .size(AVAILABLE_PARTNERS_KEY);

        assertEquals(3L, setSize, "All partners should be in the set");

        assertTrue(redisTemplate.opsForSet()
                .isMember(AVAILABLE_PARTNERS_KEY, partnerId1.toString()));
        assertTrue(redisTemplate.opsForSet()
                .isMember(AVAILABLE_PARTNERS_KEY, partnerId2.toString()));
        assertTrue(redisTemplate.opsForSet()
                .isMember(AVAILABLE_PARTNERS_KEY, partnerId3.toString()));
    }

    @Test
    void removeAvailablePartner_shouldRemovePartnerIdFromRedisSet() {
        // Given
        redisService.addAvailablePartner(TEST_PARTNER_ID);

        // When
        redisService.removeAvailablePartner(TEST_PARTNER_ID);

        // Then
        Boolean isMember = redisTemplate.opsForSet()
                .isMember(AVAILABLE_PARTNERS_KEY, TEST_PARTNER_ID.toString());

        assertFalse(isMember, "Partner should be removed from available set");
    }

    @Test
    void removeAvailablePartner_shouldNotFailIfPartnerNotInSet() {
        // When & Then
        // Should not throw exception
        assertDoesNotThrow(() -> redisService.removeAvailablePartner(TEST_PARTNER_ID));
    }

    @Test
    void removeAvailablePartner_shouldLeaveOtherPartnersUnaffected() {
        // Given
        Long partnerId1 = 1L;
        Long partnerId2 = 2L;

        redisService.addAvailablePartner(partnerId1);
        redisService.addAvailablePartner(partnerId2);

        // When
        redisService.removeAvailablePartner(partnerId1);

        // Then
        Boolean partner1Exists = redisTemplate.opsForSet()
                .isMember(AVAILABLE_PARTNERS_KEY, partnerId1.toString());
        Boolean partner2Exists = redisTemplate.opsForSet()
                .isMember(AVAILABLE_PARTNERS_KEY, partnerId2.toString());

        assertFalse(partner1Exists, "Removed partner should not exist");
        assertTrue(partner2Exists, "Other partner should still exist");
    }

    @Test
    void isPartnerAvailable_shouldReturnTrueIfPartnerInSet() {
        // Given
        redisService.addAvailablePartner(TEST_PARTNER_ID);

        // When
        boolean isAvailable = redisService.isPartnerAvailable(TEST_PARTNER_ID);

        // Then
        assertTrue(isAvailable, "Should return true for available partner");
    }

    @Test
    void isPartnerAvailable_shouldReturnFalseIfPartnerNotInSet() {
        // When
        boolean isAvailable = redisService.isPartnerAvailable(TEST_PARTNER_ID);

        // Then
        assertFalse(isAvailable, "Should return false for unavailable partner");
    }

    @Test
    void isPartnerAvailable_shouldReturnFalseAfterRemoval() {
        // Given
        redisService.addAvailablePartner(TEST_PARTNER_ID);

        // When
        redisService.removeAvailablePartner(TEST_PARTNER_ID);
        boolean isAvailable = redisService.isPartnerAvailable(TEST_PARTNER_ID);

        // Then
        assertFalse(isAvailable, "Should return false after removal");
    }

    @Test
    void clearAvailablePartners_shouldRemoveAllPartners() {
        // Given
        redisService.addAvailablePartner(1L);
        redisService.addAvailablePartner(2L);
        redisService.addAvailablePartner(3L);

        Long sizeBeforeClear = redisTemplate.opsForSet()
                .size(AVAILABLE_PARTNERS_KEY);

        assertTrue(sizeBeforeClear > 0, "Set should have partners before clear");

        // When
        redisService.clearAvailablePartners();

        // Then
        Long sizeAfterClear = redisTemplate.opsForSet()
                .size(AVAILABLE_PARTNERS_KEY);

        assertEquals(0L, sizeAfterClear, "Set should be empty after clear");
    }

    @Test
    void addAvailablePartner_idempotencyTest() {
        // When - adding same partner twice
        redisService.addAvailablePartner(TEST_PARTNER_ID);
        redisService.addAvailablePartner(TEST_PARTNER_ID);

        // Then
        Long setSize = redisTemplate.opsForSet()
                .size(AVAILABLE_PARTNERS_KEY);

        assertEquals(1L, setSize, "Redis Set should contain only one instance");
    }
}
