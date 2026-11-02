package com.fooddelivery.notification.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCreated(UUID eventId, Long orderId, Long userId, Instant occurredAt) {
}
