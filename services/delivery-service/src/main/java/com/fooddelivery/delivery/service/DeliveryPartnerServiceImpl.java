package com.fooddelivery.delivery.service;

import com.fooddelivery.delivery.dto.CreateDeliveryPartnerRequest;
import com.fooddelivery.delivery.dto.NearbyPartnerResponse;
import com.fooddelivery.delivery.entity.DeliveryPartner;
import com.fooddelivery.delivery.entity.DeliveryPartnerStatus;
import com.fooddelivery.delivery.exception.DeliveryPartnerAlreadyExistsException;
import com.fooddelivery.delivery.exception.DeliveryPartnerNotFoundException;
import com.fooddelivery.delivery.repository.DeliveryPartnerRepository;
import com.fooddelivery.delivery.service.redis.DeliveryPartnerRedisService;
import com.fooddelivery.delivery.service.redis.DeliveryPartnerGeoRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeliveryPartnerServiceImpl
        implements DeliveryPartnerService {

    private final DeliveryPartnerRepository repository;
    private final DeliveryPartnerRedisService redisService;
    private final DeliveryPartnerGeoRedisService geoRedisService;

    @Override
    public DeliveryPartner createDeliveryPartner(
            CreateDeliveryPartnerRequest request) {

        log.info(
                "Creating delivery partner for userId={}",
                request.getUserId()
        );

        if (repository.existsByUserId(request.getUserId())) {
            throw DeliveryPartnerAlreadyExistsException
                    .withUserId(request.getUserId());
        }

        DeliveryPartner partner = DeliveryPartner.builder()
                .userId(request.getUserId())
                .status(DeliveryPartnerStatus.OFFLINE)
                .build();

        DeliveryPartner saved = repository.save(partner);

        log.info(
                "Delivery partner created id={}, userId={}",
                saved.getId(),
                saved.getUserId()
        );

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryPartner getDeliveryPartner(Long id) {

        return repository.findById(id)
                .orElseThrow(
                        () -> DeliveryPartnerNotFoundException
                                .withId(id)
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryPartner> getAllDeliveryPartners() {

        return repository.findAll();
    }

    @Override
    public void deleteDeliveryPartner(Long id) {

        DeliveryPartner partner = getDeliveryPartner(id);

        // Remove the partner from Redis GEO location tracking
        geoRedisService.removeLocation(id);

        // Remove the partner from the Redis availability set
        redisService.removeAvailablePartner(id);
        
        repository.delete(partner);

        log.info(
                "Delivery partner deleted id={}",
                id
        );
    }

    @Override
    public DeliveryPartner updateAvailability(
            Long id,
            boolean available) {

        DeliveryPartner partner = getDeliveryPartner(id);

        if (partner.getStatus() == DeliveryPartnerStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Suspended delivery partner cannot change availability"
            );
        }

        if (partner.getStatus() == DeliveryPartnerStatus.BUSY
                && !available) {

            throw new IllegalStateException(
                    "Busy delivery partner cannot change availability"
            );
        }

        DeliveryPartnerStatus oldStatus = partner.getStatus();

        // Determine new status
        DeliveryPartnerStatus newStatus = available
                ? DeliveryPartnerStatus.AVAILABLE
                : DeliveryPartnerStatus.OFFLINE;

        partner.setStatus(newStatus);

        // Persist to MySQL first (durable source of truth)
        DeliveryPartner saved = repository.save(partner);

        // Update Redis based on status transition
        handleRedisUpdate(id, oldStatus, newStatus);

        log.info(
                "Delivery partner {} availability changed from {} to {}",
                id,
                oldStatus,
                newStatus
        );

        return saved;
    }

    /**
     * Handles Redis updates based on delivery partner status transitions.
     *
     * Rules:
     * - AVAILABLE status: add to Redis set
     * - OFFLINE or BUSY status: remove from Redis set
     * - SUSPENDED: no Redis operation
     *
     * @param partnerId The delivery partner ID
     * @param oldStatus The previous status
     * @param newStatus The new status
     */
    private void handleRedisUpdate(
            Long partnerId,
            DeliveryPartnerStatus oldStatus,
            DeliveryPartnerStatus newStatus) {

        // If transitioning TO AVAILABLE, add to Redis
        if (newStatus == DeliveryPartnerStatus.AVAILABLE) {
            redisService.addAvailablePartner(partnerId);
        }

        // If transitioning FROM AVAILABLE to non-AVAILABLE status, remove from Redis
        if (oldStatus == DeliveryPartnerStatus.AVAILABLE
                && newStatus != DeliveryPartnerStatus.AVAILABLE) {
            redisService.removeAvailablePartner(partnerId);
        }
    }

    @Override
    public void updateLocation(
            Long id,
            Double latitude,
            Double longitude) {

        DeliveryPartner partner = getDeliveryPartner(id);

        // SUSPENDED partners cannot update location
        if (partner.getStatus() == DeliveryPartnerStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Suspended delivery partner cannot update location"
            );
        }

        log.info(
                "Updating location for delivery partner {}: lat={}, lon={}",
                id,
                latitude,
                longitude
        );

        // Store location in Redis GEO
        // This throws RedisGeoOperationException if it fails
        geoRedisService.storeLocation(id, longitude, latitude);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbyPartnerResponse> findNearbyPartners(
            Double latitude,
            Double longitude,
            Double radiusKm) {

        log.debug(
                "Searching for nearby partners at lat={}, lon={} within {} km",
                latitude,
                longitude,
                radiusKm
        );

        // Query Redis GEO and filter by availability
        // This throws RedisGeoOperationException if it fails
        return geoRedisService.findNearbyPartners(latitude, longitude, radiusKm);
    }
}