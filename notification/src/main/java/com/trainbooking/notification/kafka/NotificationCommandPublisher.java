package com.trainbooking.notification.kafka;

import com.trainbooking.notification.domain.enums.Channel;
import com.trainbooking.notification.domain.enums.NotificationPriority;
import com.trainbooking.notification.exception.NotificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Routes a {@link NotificationCommand} to the delivery topic for its (channel, priority) pair and
 * publishes it. Keyed by {@code userId} for per-user ordering within a partition.
 */
@Component
public class NotificationCommandPublisher {

    private record Route(Channel channel, NotificationPriority priority) {}

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<Route, String> topicByRoute = new HashMap<>();

    public NotificationCommandPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${kafka.topics.email-transactional}") String emailTransactional,
            @Value("${kafka.topics.email-bulk}") String emailBulk,
            @Value("${kafka.topics.sms-transactional}") String smsTransactional) {
        this.kafkaTemplate = kafkaTemplate;
        topicByRoute.put(new Route(Channel.EMAIL, NotificationPriority.TRANSACTIONAL), emailTransactional);
        topicByRoute.put(new Route(Channel.EMAIL, NotificationPriority.BULK), emailBulk);
        topicByRoute.put(new Route(Channel.SMS, NotificationPriority.TRANSACTIONAL), smsTransactional);
    }

    public void publish(NotificationCommand command) {
        String topic = topicByRoute.get(new Route(command.channel(), command.priority()));
        if (topic == null) {
            throw new NotificationException(
                    "No delivery topic configured for " + command.channel() + "/" + command.priority());
        }
        kafkaTemplate.send(topic, String.valueOf(command.userId()), command);
    }
}
