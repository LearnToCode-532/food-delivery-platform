package com.fooddelivery.notification.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(OutputCaptureExtension.class)
class KafkaConsumerConfigurationTest {

    @Test
    void configuresSpringKafkaDefaultErrorHandler() {
        assertInstanceOf(DefaultErrorHandler.class,
                new KafkaConsumerConfiguration().kafkaErrorHandler());
    }

    @Test
    void retriesTwiceThenLogsRecordMetadataWithoutLoggingPayload(CapturedOutput output) {
        DefaultErrorHandler handler = (DefaultErrorHandler)
                new KafkaConsumerConfiguration().kafkaErrorHandler();
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("user.events", 0, 7L, "event-key", "sensitive-payload");
        Consumer<String, String> consumer = mock();
        MessageListenerContainer container = mock();
        RuntimeException failure = new IllegalStateException("processing failed");

        assertFalse(handler.handleOne(failure, record, consumer, container));
        assertFalse(handler.handleOne(failure, record, consumer, container));
        assertTrue(handler.handleOne(failure, record, consumer, container));

        assertTrue(output.getOut().contains("Kafka record recovered without a dead-letter topic"));
        assertTrue(output.getOut().contains("topic=user.events, partition=0, offset=7"));
        assertFalse(output.getOut().contains("sensitive-payload"));
    }

    @Test
    void deserializationFailureIsRecoveredWithoutRetryAndAllowsOffsetCommit(CapturedOutput output) {
        DefaultErrorHandler handler = (DefaultErrorHandler)
                new KafkaConsumerConfiguration().kafkaErrorHandler();
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("user.events", 0, 8L, "event-key", null);
        Consumer<String, String> consumer = mock();
        MessageListenerContainer container = mock();
        DeserializationException failure = new DeserializationException(
                "Malformed event JSON", new byte[] {1}, false,
                new IllegalArgumentException("Invalid JSON"));

        assertTrue(handler.handleOne(failure, record, consumer, container));
        assertTrue(handler.isAckAfterHandle());
        assertTrue(output.getOut().contains("Kafka record recovered without a dead-letter topic"));
        assertTrue(output.getOut().contains("topic=user.events, partition=0, offset=8"));
    }
}
