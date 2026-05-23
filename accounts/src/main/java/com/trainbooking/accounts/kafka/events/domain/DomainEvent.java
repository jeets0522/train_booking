package com.trainbooking.accounts.kafka.events.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

/**
 * Base type for facts emitted by the accounts service onto the {@code account.events} topic.
 *
 * <p>The concrete type is carried in the JSON body via the {@code eventType} discriminator
 * (Kafka type headers are disabled), so a single topic can carry many event types and the
 * notification service can resolve the subtype on consume.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UserRegistered.class, name = "UserRegistered"),
        @JsonSubTypes.Type(value = EmailVerificationRequested.class, name = "EmailVerificationRequested"),
        @JsonSubTypes.Type(value = PhoneOtpRequested.class, name = "PhoneOtpRequested")
})
public sealed interface DomainEvent
        permits UserRegistered, EmailVerificationRequested, PhoneOtpRequested {

    /** Unique per emitted event; downstream derives idempotency keys from it. */
    UUID eventId();

    /** Subject of the event. Used as the Kafka partition key for per-user ordering. */
    Long userId();

    Instant occurredAt();
}
