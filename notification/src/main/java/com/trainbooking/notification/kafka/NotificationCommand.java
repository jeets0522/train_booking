package com.trainbooking.notification.kafka;

import com.trainbooking.notification.domain.enums.Channel;
import com.trainbooking.notification.domain.enums.NotificationPriority;

import java.util.Map;
import java.util.UUID;

/**
 * A resolved delivery instruction produced by {@link com.trainbooking.notification.policy.NotificationPolicy}
 * from a domain event. Carried on the channel/priority delivery topics and consumed by the
 * {@code DeliveryConsumer}, which maps it to a {@code NotificationRequest}.
 *
 * @param id deterministic notification id (drives idempotent {@code NotificationService.send})
 */
public record NotificationCommand(
        UUID id,
        Channel channel,
        NotificationPriority priority,
        String recipient,
        String templateKey,
        Map<String, Object> variables,
        Long userId) {
}
