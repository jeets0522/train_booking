package com.trainbooking.accounts.kafka;

import com.trainbooking.accounts.kafka.events.domain.DomainEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes account domain events to the {@code account.events} topic. The accounts service
 * states facts only; it does not know which notifications (if any) result from them.
 */
@Service
public class DomainEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.account-events}")
    private String accountEventsTopic;

    public DomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(DomainEvent event) {
        kafkaTemplate.send(accountEventsTopic, event.userId().toString(), event);
    }
}
