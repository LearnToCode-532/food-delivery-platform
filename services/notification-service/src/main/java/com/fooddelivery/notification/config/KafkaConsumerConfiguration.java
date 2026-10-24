package com.fooddelivery.notification.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class KafkaConsumerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfiguration.class);

    @Bean
    CommonErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(
                this::logRecoveredRecord,
                new FixedBackOff(1_000L, 2L));
    }

    private void logRecoveredRecord(ConsumerRecord<?, ?> record, Exception exception) {
        log.error("Kafka record recovered without a dead-letter topic; it will be acknowledged: "
                        + "topic={}, partition={}, offset={}",
                record.topic(), record.partition(), record.offset(), exception);
    }
}
