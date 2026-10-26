package com.fooddelivery.catalog.menu.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MenuItemAvailabilityRequest {

    @NotNull
    private Boolean available;
}
