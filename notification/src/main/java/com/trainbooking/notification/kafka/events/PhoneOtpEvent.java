package com.trainbooking.notification.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Mirror of accounts.kafka.events.PhoneOtpEvent.
 * Reserved for the future SMS channel; not consumed in v1.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhoneOtpEvent {
    private Long userId;
    private String phoneNumber;
    private String otp;
    private Instant expiresAt;
}
