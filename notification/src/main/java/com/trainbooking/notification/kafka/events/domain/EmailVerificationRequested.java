package com.trainbooking.notification.kafka.events.domain;

import java.time.Instant;
import java.util.UUID;

public record EmailVerificationRequested(
        UUID eventId,
        Long userId,
        Instant occurredAt,
        String email,
        String firstName,
        String rawToken,
        Instant expiresAt) implements DomainEvent {
}
