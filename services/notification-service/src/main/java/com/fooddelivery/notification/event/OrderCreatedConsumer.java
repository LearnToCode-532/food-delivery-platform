package com.fooddelivery.notification.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    @KafkaListener(
            topics = "${app.kafka.topics.order-events}",
            properties = {
                    "spring.json.value.default.type=com.fooddelivery.notification.event.OrderCreated",
                    "spring.json.trusted.packages=com.fooddelivery.notification.event",
                    "spring.json.use.type.headers=false"
            })
    public void consume(OrderCreated event) {
        if (event == null
                || event.eventId() == null
                || event.orderId() == null
                || event.orderId() <= 0
                || event.userId() == null
                || event.userId() <= 0
                || event.occurredAt() == null) {
            throw new IllegalArgumentException("OrderCreated event is missing required fields");
        }

        log.info("Received OrderCreated event {} for order {} and user {}",
                event.eventId(), event.orderId(), event.userId());
    }
}
