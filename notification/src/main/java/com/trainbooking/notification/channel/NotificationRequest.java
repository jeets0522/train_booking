package com.trainbooking.notification.channel;

import com.trainbooking.notification.domain.enums.Channel;

import java.util.Map;
import java.util.UUID;

public record NotificationRequest(
        UUID id,
        Channel channel,
        String recipient,
        String templateKey,
        Map<String, Object> variables,
        Long userId) {
}
