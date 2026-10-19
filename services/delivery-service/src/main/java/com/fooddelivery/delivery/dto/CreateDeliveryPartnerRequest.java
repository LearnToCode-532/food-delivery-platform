package com.fooddelivery.delivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDeliveryPartnerRequest {

    @NotNull
    private Long userId;
}