package com.trainbooking.accounts.kafka.events.domain;

import java.time.Instant;
import java.util.UUID;

public record PhoneOtpRequested(
        UUID eventId,
        Long userId,
        Instant occurredAt,
        String phoneNumber,
        String otp,
        Instant expiresAt) implements DomainEvent {

    public static PhoneOtpRequested of(
            Long userId, String phoneNumber, String otp, Instant expiresAt) {
        return new PhoneOtpRequested(
                UUID.randomUUID(), userId, Instant.now(), phoneNumber, otp, expiresAt);
    }
}
