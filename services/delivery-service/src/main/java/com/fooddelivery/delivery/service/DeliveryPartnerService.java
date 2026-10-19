package com.fooddelivery.delivery.service;

import com.fooddelivery.delivery.dto.CreateDeliveryPartnerRequest;
import com.fooddelivery.delivery.dto.NearbyPartnerResponse;
import com.fooddelivery.delivery.entity.DeliveryPartner;

import java.util.List;

public interface DeliveryPartnerService {

    DeliveryPartner createDeliveryPartner(
            CreateDeliveryPartnerRequest request);

    DeliveryPartner getDeliveryPartner(Long id);

    List<DeliveryPartner> getAllDeliveryPartners();

    void deleteDeliveryPartner(Long id);

    DeliveryPartner updateAvailability(
            Long id,
            boolean available);

    void updateLocation(
            Long id,
            Double latitude,
            Double longitude);

    List<NearbyPartnerResponse> findNearbyPartners(
            Double latitude,
            Double longitude,
            Double radiusKm);
}