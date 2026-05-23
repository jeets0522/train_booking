package com.trainbooking.notification.kafka.events.domain;

import java.time.Instant;
import java.util.UUID;

public record PhoneOtpRequested(
        UUID eventId,
        Long userId,
        Instant occurredAt,
        String phoneNumber,
        String otp,
        Instant expiresAt) implements DomainEvent {
}
