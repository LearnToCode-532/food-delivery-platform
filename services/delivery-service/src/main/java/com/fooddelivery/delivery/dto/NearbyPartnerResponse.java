package com.fooddelivery.delivery.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO for a nearby delivery partner search result.
 *
 * Contains the partner ID and distance from the search center point.
 */
@Getter
@Builder
public class NearbyPartnerResponse {

    /**
     * The delivery partner ID.
     */
    private Long partnerId;

    /**
     * Distance from the search center point, in kilometers.
     */
    private Double distanceKm;
}
