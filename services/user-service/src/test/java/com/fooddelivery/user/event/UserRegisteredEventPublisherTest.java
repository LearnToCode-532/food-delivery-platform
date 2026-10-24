package com.fooddelivery.user.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.KafkaException;
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
class UserRegisteredEventPublisherTest {

    private KafkaTemplate<String, UserRegistered> kafkaTemplate;
    private UserRegisteredEventPublisher publisher;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock();
        publisher = new UserRegisteredEventPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(publisher, "topic", "user.events");
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
    void publishAfterCommitWaitsUntilTransactionCommitsAndSendsPersistedUserId() {
        when(kafkaTemplate.send(eq("user.events"), anyString(), any(UserRegistered.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publishAfterCommit(42L);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(UserRegistered.class));

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(kafkaTemplate).send(eq("user.events"), anyString(),
                org.mockito.ArgumentMatchers.argThat(event ->
                        event.userId().equals(42L)
                                && event.eventId() != null
                                && event.occurredAt() != null));
    }

    @Test
    void rolledBackTransactionDoesNotPublishEvent() {
        publisher.publishAfterCommit(42L);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization ->
                        synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(UserRegistered.class));
    }

    @Test
    void asynchronousKafkaFailureDoesNotUndoCommittedRegistrationAndIsLogged(CapturedOutput output) {
        when(kafkaTemplate.send(eq("user.events"), anyString(), any(UserRegistered.class)))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("broker unavailable")));

        publisher.publishAfterCommit(42L);

        assertDoesNotThrow(() -> TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit));
        verify(kafkaTemplate).send(eq("user.events"), anyString(), any(UserRegistered.class));
        org.junit.jupiter.api.Assertions.assertTrue(
                output.getOut().contains("Kafka send failed for UserRegistered event")
                        && output.getOut().contains("and user 42"));
    }

    @Test
    void synchronousKafkaFailureDoesNotEscapeThePostCommitCallbackAndIsLogged(CapturedOutput output) {
        when(kafkaTemplate.send(eq("user.events"), anyString(), any(UserRegistered.class)))
                .thenThrow(new KafkaException("broker metadata unavailable"));

        publisher.publishAfterCommit(42L);

        assertDoesNotThrow(() -> TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit));
        verify(kafkaTemplate).send(eq("user.events"), anyString(), any(UserRegistered.class));
        org.junit.jupiter.api.Assertions.assertTrue(
                output.getOut().contains("Could not submit UserRegistered event"));
    }

    @Test
    void eventContractContainsOnlyEventIdUserIdAndOccurrenceTime() {
        UserRegistered event = new UserRegistered(UUID.randomUUID(), 42L, java.time.Instant.now());

        assertEquals(3, UserRegistered.class.getRecordComponents().length);
        assertEquals(42L, event.userId());
    }
}
