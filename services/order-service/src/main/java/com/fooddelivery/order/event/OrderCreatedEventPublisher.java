package com.fooddelivery.order.event;

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
public class OrderCreatedEventPublisher {

    private final KafkaTemplate<String, OrderCreated> kafkaTemplate;

    @Value("${app.kafka.topics.order-events}")
    private String topic;

    public void publishAfterCommit(Long orderId, Long userId) {
        OrderCreated event = new OrderCreated(UUID.randomUUID(), orderId, userId, Instant.now());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                send(event);
            }
        });
    }

    private void send(OrderCreated event) {
        try {
            kafkaTemplate.send(topic, event.eventId().toString(), event)
                    .whenComplete((result, failure) -> logSendResult(event, result, failure));
        } catch (RuntimeException exception) {
            log.error("Could not submit OrderCreated event {} for order {} and user {} to Kafka",
                    event.eventId(), event.orderId(), event.userId(), exception);
        }
    }

    private void logSendResult(
            OrderCreated event,
            SendResult<String, OrderCreated> result,
            Throwable failure) {
        if (failure != null) {
            log.error("Kafka send failed for OrderCreated event {} for order {} and user {}",
                    event.eventId(), event.orderId(), event.userId(), failure);
            return;
        }

        log.info("Published OrderCreated event {} for order {} and user {} to {} partition {} offset {}",
                event.eventId(), event.orderId(), event.userId(), result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
    }
}
