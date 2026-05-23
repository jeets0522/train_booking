package com.trainbooking.notification.kafka;

import com.trainbooking.notification.kafka.events.domain.DomainEvent;
import com.trainbooking.notification.policy.NotificationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stage 1 of delivery: consumes domain facts from {@code account.events}, asks {@link
 * NotificationPolicy} which notifications they imply, and republishes each resulting command onto
 * its channel/priority delivery topic. The concrete event subtype is resolved from the
 * {@code eventType} discriminator in the JSON body.
 */
@Component
public class DomainEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DomainEventConsumer.class);

    private final NotificationPolicy policy;
    private final NotificationCommandPublisher commandPublisher;

    public DomainEventConsumer(NotificationPolicy policy, NotificationCommandPublisher commandPublisher) {
        this.policy = policy;
        this.commandPublisher = commandPublisher;
    }

    @KafkaListener(
            topics = "${kafka.topics.account-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {"spring.json.value.default.type=com.trainbooking.notification.kafka.events.domain.DomainEvent"})
    public void onDomainEvent(DomainEvent event) {
        List<NotificationCommand> commands = policy.apply(event);
        log.info("Domain event {} (eventId={}, userId={}) -> {} command(s)",
                event.getClass().getSimpleName(), event.eventId(), event.userId(), commands.size());
        for (NotificationCommand command : commands) {
            commandPublisher.publish(command);
        }
    }
}
