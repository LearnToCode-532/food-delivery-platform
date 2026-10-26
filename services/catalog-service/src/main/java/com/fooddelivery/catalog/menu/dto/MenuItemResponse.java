package com.fooddelivery.catalog.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemResponse {

    private Long id;
    private Long restaurantId;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private boolean available;
    private Instant createdAt;
    private Instant updatedAt;
}
