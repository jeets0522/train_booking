package com.trainbooking.accounts.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
