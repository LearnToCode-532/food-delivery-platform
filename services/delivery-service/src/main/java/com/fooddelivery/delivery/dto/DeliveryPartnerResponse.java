package com.fooddelivery.delivery.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

import com.fooddelivery.delivery.entity.DeliveryPartnerStatus;

@Getter
@Builder
public class DeliveryPartnerResponse {

    private Long id;
    private Long userId;
    private DeliveryPartnerStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}