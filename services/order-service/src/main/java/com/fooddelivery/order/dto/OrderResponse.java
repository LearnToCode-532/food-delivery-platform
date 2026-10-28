package com.fooddelivery.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        Long cartId,
        String status,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        String currency,
        Instant createdAt,
        Instant updatedAt) {
}
