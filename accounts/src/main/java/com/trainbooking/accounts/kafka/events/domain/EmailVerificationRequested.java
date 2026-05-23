package com.trainbooking.accounts.kafka.events.domain;

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

    public static EmailVerificationRequested of(
            Long userId, String email, String firstName, String rawToken, Instant expiresAt) {
        return new EmailVerificationRequested(
                UUID.randomUUID(), userId, Instant.now(), email, firstName, rawToken, expiresAt);
    }
}
