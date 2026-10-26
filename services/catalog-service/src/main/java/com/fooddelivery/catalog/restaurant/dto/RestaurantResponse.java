package com.fooddelivery.catalog.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantResponse {

    private Long id;
    private String name;
    private String description;
    private String address;
    private String phoneNumber;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
