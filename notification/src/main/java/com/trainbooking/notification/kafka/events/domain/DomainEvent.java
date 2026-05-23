package com.trainbooking.notification.kafka.events.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumer-side mirror of the accounts {@code DomainEvent} hierarchy. Structurally byte-compatible
 * with the producer (cross-service POJOs are duplicated, not shared). The {@code eventType}
 * discriminator in the JSON body selects the subtype on deserialization.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UserRegistered.class, name = "UserRegistered"),
        @JsonSubTypes.Type(value = EmailVerificationRequested.class, name = "EmailVerificationRequested"),
        @JsonSubTypes.Type(value = PhoneOtpRequested.class, name = "PhoneOtpRequested")
})
public sealed interface DomainEvent
        permits UserRegistered, EmailVerificationRequested, PhoneOtpRequested {

    /** Unique per emitted event; notification idempotency keys are derived from it. */
    UUID eventId();

    Long userId();

    Instant occurredAt();
}
