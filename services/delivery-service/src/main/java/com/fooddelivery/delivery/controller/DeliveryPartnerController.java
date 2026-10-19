package com.fooddelivery.delivery.controller;

import com.fooddelivery.delivery.dto.CreateDeliveryPartnerRequest;
import com.fooddelivery.delivery.dto.DeliveryPartnerResponse;
import com.fooddelivery.delivery.dto.NearbyPartnerResponse;
import com.fooddelivery.delivery.dto.UpdateAvailabilityRequest;
import com.fooddelivery.delivery.dto.UpdateLocationRequest;
import com.fooddelivery.delivery.entity.DeliveryPartner;
import com.fooddelivery.delivery.service.DeliveryPartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/delivery-partners")
@RequiredArgsConstructor
@Slf4j
public class DeliveryPartnerController {

    private final DeliveryPartnerService deliveryPartnerService;

    @PostMapping
    public ResponseEntity<DeliveryPartnerResponse> create(
            @Valid @RequestBody CreateDeliveryPartnerRequest request) {

        DeliveryPartner partner =
                deliveryPartnerService.createDeliveryPartner(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(partner));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryPartnerResponse> getById(
            @PathVariable Long id) {

        DeliveryPartner partner =
                deliveryPartnerService.getDeliveryPartner(id);

        return ResponseEntity.ok(toResponse(partner));
    }

    @GetMapping
    public ResponseEntity<List<DeliveryPartnerResponse>> getAll() {

        List<DeliveryPartnerResponse> response =
                deliveryPartnerService.getAllDeliveryPartners()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyPartnerResponse>> findNearby(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double radiusKm) {

        List<NearbyPartnerResponse> response =
                deliveryPartnerService.findNearbyPartners(
                        latitude,
                        longitude,
                        radiusKm
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<DeliveryPartnerResponse> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAvailabilityRequest request) {

        DeliveryPartner partner =
                deliveryPartnerService.updateAvailability(
                        id,
                        request.getAvailable()
                );

        return ResponseEntity.ok(toResponse(partner));
    }

    @PutMapping("/{id}/location")
    public ResponseEntity<Void> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLocationRequest request) {

        deliveryPartnerService.updateLocation(
                id,
                request.getLatitude(),
                request.getLongitude()
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        deliveryPartnerService.deleteDeliveryPartner(id);

        return ResponseEntity.noContent().build();
    }

    private DeliveryPartnerResponse toResponse(
            DeliveryPartner partner) {

        return DeliveryPartnerResponse.builder()
                .id(partner.getId())
                .userId(partner.getUserId())
                .status(partner.getStatus())
                .createdAt(partner.getCreatedAt())
                .updatedAt(partner.getUpdatedAt())
                .build();
    }
}