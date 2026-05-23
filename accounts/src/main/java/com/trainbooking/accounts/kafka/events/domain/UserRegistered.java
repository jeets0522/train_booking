package com.trainbooking.accounts.kafka.events.domain;

import java.time.Instant;
import java.util.UUID;

public record UserRegistered(
        UUID eventId,
        Long userId,
        Instant occurredAt,
        String email,
        String firstName) implements DomainEvent {

    public static UserRegistered of(Long userId, String email, String firstName) {
        return new UserRegistered(UUID.randomUUID(), userId, Instant.now(), email, firstName);
    }
}
