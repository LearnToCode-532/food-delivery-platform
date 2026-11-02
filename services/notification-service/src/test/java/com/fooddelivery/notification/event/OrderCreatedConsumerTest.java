package com.fooddelivery.notification.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class OrderCreatedConsumerTest {

    private final OrderCreatedConsumer consumer = new OrderCreatedConsumer();

    @Test
    void validEventLogsOnlyEventOrderAndUserIdentifiers(CapturedOutput output) {
        UUID eventId = UUID.randomUUID();

        consumer.consume(new OrderCreated(eventId, 42L, 11L, Instant.now()));

        assertTrue(output.getOut().contains(eventId.toString()));
        assertTrue(output.getOut().contains("order 42"));
        assertTrue(output.getOut().contains("user 11"));
    }

    @Test
    void missingEventFieldsAreRejectedWithoutLoggingPayload() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> consumer.consume(new OrderCreated(null, 42L, 11L, Instant.now())));

        assertTrue(exception.getMessage().contains("missing required fields"));
    }
}
