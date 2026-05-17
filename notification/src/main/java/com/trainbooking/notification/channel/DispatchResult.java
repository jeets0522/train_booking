package com.trainbooking.notification.channel;

import com.trainbooking.notification.domain.enums.NotificationStatus;
import com.trainbooking.notification.domain.enums.ProviderName;

import java.util.UUID;

public record DispatchResult(
        UUID notificationId,
        NotificationStatus status,
        ProviderName providerUsed,
        String providerMessageId,
        String errorMessage) {

    public static DispatchResult sent(UUID id, ProviderName provider, String messageId) {
        return new DispatchResult(id, NotificationStatus.SENT, provider, messageId, null);
    }

    public static DispatchResult failed(UUID id, String error) {
        return new DispatchResult(id, NotificationStatus.FAILED, null, null, error);
    }

    public static DispatchResult suppressed(UUID id) {
        return new DispatchResult(id, NotificationStatus.SUPPRESSED, null, null, null);
    }
}
