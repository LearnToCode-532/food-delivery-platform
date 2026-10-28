package com.fooddelivery.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull(message = "cartId is required")
        @Min(value = 1, message = "cartId must be at least 1")
        Long cartId) {
}
