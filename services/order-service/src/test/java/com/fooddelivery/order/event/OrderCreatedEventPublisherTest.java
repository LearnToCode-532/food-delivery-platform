package com.fooddelivery.order.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class OrderCreatedEventPublisherTest {

    private KafkaTemplate<String, OrderCreated> kafkaTemplate;
    private OrderCreatedEventPublisher publisher;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock();
        publisher = new OrderCreatedEventPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(publisher, "topic", "order.events");
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void publishAfterCommitWaitsUntilTransactionCommitsAndSendsPersistedOrderId() {
        when(kafkaTemplate.send(eq("order.events"), anyString(), any(OrderCreated.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publishAfterCommit(42L, 11L);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(OrderCreated.class));

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(kafkaTemplate).send(eq("order.events"), anyString(),
                org.mockito.ArgumentMatchers.argThat(event ->
                        event.userId().equals(11L)
                                && event.orderId().equals(42L)
                                && event.eventId() != null
                                && event.occurredAt() != null));
    }

    @Test
    void rolledBackTransactionDoesNotPublishEvent() {
        publisher.publishAfterCommit(42L, 11L);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization ->
                        synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(OrderCreated.class));
    }

    @Test
    void asynchronousKafkaFailureDoesNotUndoCommittedOrderAndIsLogged(CapturedOutput output) {
        when(kafkaTemplate.send(eq("order.events"), anyString(), any(OrderCreated.class)))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("broker unavailable")));

        publisher.publishAfterCommit(42L, 11L);

        assertDoesNotThrow(() -> TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit));
        verify(kafkaTemplate).send(eq("order.events"), anyString(), any(OrderCreated.class));
        org.junit.jupiter.api.Assertions.assertTrue(
                output.getOut().contains("Kafka send failed for OrderCreated event")
                        && output.getOut().contains("order 42")
                        && output.getOut().contains("user 11"));
    }

    @Test
    void synchronousKafkaFailureDoesNotEscapeThePostCommitCallbackAndIsLogged(CapturedOutput output) {
        when(kafkaTemplate.send(eq("order.events"), anyString(), any(OrderCreated.class)))
                .thenThrow(new KafkaException("broker metadata unavailable"));

        publisher.publishAfterCommit(42L, 11L);

        assertDoesNotThrow(() -> TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit));
        verify(kafkaTemplate).send(eq("order.events"), anyString(), any(OrderCreated.class));
        org.junit.jupiter.api.Assertions.assertTrue(
                output.getOut().contains("Could not submit OrderCreated event"));
    }

    @Test
    void eventContractContainsOnlyEventIdOrderIdUserIdAndOccurrenceTime() {
        OrderCreated event = new OrderCreated(UUID.randomUUID(), 42L, 11L, java.time.Instant.now());

        assertEquals(4, OrderCreated.class.getRecordComponents().length);
        assertEquals(42L, event.orderId());
        assertEquals(11L, event.userId());
    }
}
