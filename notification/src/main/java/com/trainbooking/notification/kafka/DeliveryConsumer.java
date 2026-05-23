package com.trainbooking.notification.kafka;

import com.trainbooking.notification.channel.NotificationRequest;
import com.trainbooking.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Stage 2 of delivery: consumes resolved {@link NotificationCommand}s from the channel/priority
 * topics and hands them to {@link NotificationService#send}. One listener per topic so each
 * (channel, priority) tier can be scaled/tuned independently — transactional sends are not stalled
 * behind bulk backlogs.
 */
@Component
public class DeliveryConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryConsumer.class);

    private static final String COMMAND_TYPE =
            "spring.json.value.default.type=com.trainbooking.notification.kafka.NotificationCommand";

    private final NotificationService notificationService;

    public DeliveryConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "${kafka.topics.email-transactional}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {COMMAND_TYPE})
    public void onEmailTransactional(NotificationCommand command) {
        dispatch(command);
    }

    @KafkaListener(
            topics = "${kafka.topics.email-bulk}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {COMMAND_TYPE})
    public void onEmailBulk(NotificationCommand command) {
        dispatch(command);
    }

    @KafkaListener(
            topics = "${kafka.topics.sms-transactional}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {COMMAND_TYPE})
    public void onSmsTransactional(NotificationCommand command) {
        dispatch(command);
    }

    private void dispatch(NotificationCommand command) {
        log.info("Delivering command id={} template={} channel={} priority={}",
                command.id(), command.templateKey(), command.channel(), command.priority());
        notificationService.send(new NotificationRequest(
                command.id(),
                command.channel(),
                command.recipient(),
                command.templateKey(),
                command.variables(),
                command.userId()));
    }
}
