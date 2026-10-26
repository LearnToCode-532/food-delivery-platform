package com.fooddelivery.catalog.restaurant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RestaurantActivationRequest {

    @NotNull
    private Boolean active;
}
