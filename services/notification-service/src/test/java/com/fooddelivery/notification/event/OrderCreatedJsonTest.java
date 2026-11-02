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

class OrderCreatedJsonTest {

    @Test
    void jsonWithoutTypeHeadersDeserializesIntoTheLocalEventContract() {
        UUID eventId = UUID.randomUUID();
        OrderCreated original = new OrderCreated(eventId, 42L, 11L, Instant.now());
        JacksonJsonSerializer<OrderCreated> serializer = new JacksonJsonSerializer<>();
        serializer.configure(Map.of(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false), false);
        Headers headers = new RecordHeaders();
        byte[] json = serializer.serialize("order.events", headers, original);

        OrderCreated deserialized = deserializer().deserialize("order.events", headers, json);

        assertEquals(eventId, deserialized.eventId());
        assertEquals(42L, deserialized.orderId());
        assertEquals(11L, deserialized.userId());
        assertEquals(original.occurredAt(), deserialized.occurredAt());
        assertEquals(0, headers.toArray().length);
    }

    @Test
    void malformedJsonIsRecordedForTheConfiguredErrorHandler() {
        Headers headers = new RecordHeaders();

        OrderCreated deserialized = deserializer().deserialize(
                "order.events", headers, "not-json".getBytes(StandardCharsets.UTF_8));

        assertNull(deserialized);
        assertEquals(1, headers.toArray().length);
    }

    private ErrorHandlingDeserializer<OrderCreated> deserializer() {
        ErrorHandlingDeserializer<OrderCreated> deserializer =
                new ErrorHandlingDeserializer<>(new JacksonJsonDeserializer<>(OrderCreated.class));
        deserializer.configure(Map.of(
                JacksonJsonDeserializer.TRUSTED_PACKAGES, OrderCreated.class.getPackageName(),
                JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false), false);
        return deserializer;
    }
}
