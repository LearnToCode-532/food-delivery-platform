package com.fooddelivery.notification.event;

import java.time.Instant;
import java.util.UUID;

public record UserRegistered(UUID eventId, Long userId, Instant occurredAt) {
}
