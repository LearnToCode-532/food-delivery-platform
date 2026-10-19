package com.fooddelivery.user.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredEventPublisher {

    private final KafkaTemplate<String, UserRegistered> kafkaTemplate;

    @Value("${app.kafka.topics.user-events}")
    private String topic;

    public void publishAfterCommit(Long userId) {
        UserRegistered event = new UserRegistered(UUID.randomUUID(), userId, Instant.now());
        UserRegisteredEventPublisher publisher = this;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publisher.send(event);
            }
        });
    }

    private void send(UserRegistered event) {
        try {
            kafkaTemplate.send(topic, event.eventId().toString(), event)
                    .whenComplete((result, failure) -> logSendResult(event, result, failure));
        } catch (RuntimeException exception) {
            log.error("Could not submit UserRegistered event {} for user {} to Kafka",
                    event.eventId(), event.userId(), exception);
        }
    }

    private void logSendResult(
            UserRegistered event,
            SendResult<String, UserRegistered> result,
            Throwable failure) {
        if (failure != null) {
            log.error("Kafka send failed for UserRegistered event {} and user {}",
                    event.eventId(), event.userId(), failure);
            return;
        }

        log.info("Published UserRegistered event {} for user {} to {} partition {} offset {}",
                event.eventId(), event.userId(), result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
    }
}
