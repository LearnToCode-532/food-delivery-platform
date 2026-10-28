package com.fooddelivery.order.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CartResponse(
        Long id,
        Long userId,
        List<CartItemResponse> items,
        BigDecimal subtotal,
        String currency,
        Instant createdAt,
        Instant updatedAt) {
    public CartResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
