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
class UserRegisteredConsumerTest {

    private final UserRegisteredConsumer consumer = new UserRegisteredConsumer();

    @Test
    void validEventLogsOnlyEventAndUserIdentifiers(CapturedOutput output) {
        UUID eventId = UUID.randomUUID();

        consumer.consume(new UserRegistered(eventId, 42L, Instant.now()));

        assertTrue(output.getOut().contains(eventId.toString()));
        assertTrue(output.getOut().contains("user 42"));
    }

    @Test
    void missingEventFieldsAreRejectedWithoutLoggingPayload() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> consumer.consume(new UserRegistered(null, 42L, Instant.now())));

        assertTrue(exception.getMessage().contains("missing required fields"));
    }
}
