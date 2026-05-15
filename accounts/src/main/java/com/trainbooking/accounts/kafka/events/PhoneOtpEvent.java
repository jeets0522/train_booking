package com.trainbooking.accounts.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhoneOtpEvent {

    private Long userId;
    private String phoneNumber;
    private String otp;
    private Instant expiresAt;
}
