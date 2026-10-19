package com.fooddelivery.notification.event;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserRegisteredJsonTest {

    @Test
    void jsonWithoutTypeHeadersDeserializesIntoTheLocalEventContract() {
        UUID eventId = UUID.randomUUID();
        UserRegistered original = new UserRegistered(eventId, 42L, Instant.now());
        JacksonJsonSerializer<UserRegistered> serializer = new JacksonJsonSerializer<>();
        serializer.configure(Map.of(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false), false);
        Headers headers = new RecordHeaders();
        byte[] json = serializer.serialize("user.events", headers, original);

        UserRegistered deserialized = deserializer().deserialize("user.events", headers, json);

        assertEquals(eventId, deserialized.eventId());
        assertEquals(42L, deserialized.userId());
        assertEquals(original.occurredAt(), deserialized.occurredAt());
        assertEquals(0, headers.toArray().length);
    }

    @Test
    void malformedJsonIsRecordedForTheConfiguredErrorHandler() {
        Headers headers = new RecordHeaders();

        UserRegistered deserialized = deserializer().deserialize(
                "user.events", headers, "not-json".getBytes(StandardCharsets.UTF_8));

        assertNull(deserialized);
        assertEquals(1, headers.toArray().length);
    }

    private ErrorHandlingDeserializer<UserRegistered> deserializer() {
        ErrorHandlingDeserializer<UserRegistered> deserializer =
                new ErrorHandlingDeserializer<>(new JacksonJsonDeserializer<>(UserRegistered.class));
        deserializer.configure(Map.of(
                JacksonJsonDeserializer.TRUSTED_PACKAGES, UserRegistered.class.getPackageName(),
                JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false), false);
        return deserializer;
    }
}
