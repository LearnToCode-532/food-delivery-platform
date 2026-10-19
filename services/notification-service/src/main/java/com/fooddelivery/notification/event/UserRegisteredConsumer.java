package com.fooddelivery.notification.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class UserRegisteredConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredConsumer.class);

    @KafkaListener(topics = "${app.kafka.topics.user-events}")
    public void consume(UserRegistered event) {
        if (event == null
                || event.eventId() == null
                || event.userId() == null
                || event.userId() <= 0
                || event.occurredAt() == null) {
            throw new IllegalArgumentException("UserRegistered event is missing required fields");
        }

        log.info("Received UserRegistered event {} for user {}",
                event.eventId(), event.userId());
    }
}
