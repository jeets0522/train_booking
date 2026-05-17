package com.trainbooking.notification.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirror of accounts.kafka.events.WelcomeEmailEvent.
 * Fields must stay byte-compatible for JSON deserialization across services.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WelcomeEmailEvent {
    private Long userId;
    private String email;
    private String firstName;
}
