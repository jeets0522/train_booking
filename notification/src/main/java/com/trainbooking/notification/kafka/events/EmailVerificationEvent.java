package com.trainbooking.notification.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Mirror of accounts.kafka.events.EmailVerificationEvent.
 * Fields must stay byte-compatible for JSON deserialization across services.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailVerificationEvent {
    private Long userId;
    private String email;
    private String firstName;
    private String rawToken;
    private Instant expiresAt;
}
