package com.trainbooking.notification.dto.response;

import com.trainbooking.notification.domain.entity.Notification;
import com.trainbooking.notification.domain.enums.Channel;
import com.trainbooking.notification.domain.enums.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        Channel channel,
        String recipient,
        String templateKey,
        NotificationStatus status,
        String providerUsed,
        String providerMessageId,
        int attempts,
        String lastError,
        Long userId,
        Instant createdAt,
        Instant sentAt,
        Instant deliveredAt,
        Instant failedAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getChannel(),
                n.getRecipient(),
                n.getTemplateKey(),
                n.getStatus(),
                n.getProviderUsed(),
                n.getProviderMessageId(),
                n.getAttempts(),
                n.getLastError(),
                n.getUserId(),
                n.getCreatedAt(),
                n.getSentAt(),
                n.getDeliveredAt(),
                n.getFailedAt());
    }
}
